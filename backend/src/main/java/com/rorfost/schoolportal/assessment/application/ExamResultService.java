package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.assessment.api.ExamResultResponse;
import com.rorfost.schoolportal.assessment.api.ExamResultSubjectResponse;
import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultRepository;
import com.rorfost.schoolportal.school.domain.AnnualExamResultSubject;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamResultService {
  public static final String ANNUAL = "ANNUAL";
  public static final String EKAM_KASOTI = "EKAM_KASOTI";

  private final AnnualExamResultRepository resultRepository;
  private final SchoolRepository schoolRepository;
  private final ResultPresentationSettingsRepository presentationSettings;
  private final StandardRepository standards;
  private final StandardSubjectRepository standardSubjects;
  private final SubjectRepository subjects;
  private final DataFormatter dataFormatter = new DataFormatter();

  public ExamResultService(
      AnnualExamResultRepository resultRepository,
      SchoolRepository schoolRepository,
      ResultPresentationSettingsRepository presentationSettings,
      StandardRepository standards,
      StandardSubjectRepository standardSubjects,
      SubjectRepository subjects) {
    this.resultRepository = resultRepository;
    this.schoolRepository = schoolRepository;
    this.presentationSettings = presentationSettings;
    this.standards = standards;
    this.standardSubjects = standardSubjects;
    this.subjects = subjects;
  }

  @Transactional(readOnly = true)
  public ExamResultResponse getResult(String standard, Integer rollNumber, String resultType) {
    School school = getSchool();
    return resultRepository
        .findBySchoolIdAndResultTypeAndStandardAndRollNumber(
            school.getId(), validateResultType(resultType), standard, rollNumber)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
  }

  /**
   * The owner-approved workbook has the same column layout for Exam and Ekam Kasoti results. The
   * selected result type is kept separately so an upload replaces only its own previous results.
   */
  @Transactional
  public void processExcelUpload(MultipartFile file, Integer totalWorkingDays, String resultType) {
    if (totalWorkingDays == null || totalWorkingDays < 1) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "validation_failed");
    }

    String validatedResultType = validateResultType(resultType);
    School school = getSchool();
    ResultPresentationSettings settings =
        presentationSettings
            .findById(school.getId())
            .orElseGet(() -> new ResultPresentationSettings(school.getId()));
    List<AnnualExamResult> results =
        parseWorkbook(file, school, totalWorkingDays, validatedResultType, settings);
    if (results.isEmpty()) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }

    resultRepository.deleteBySchoolIdAndResultType(school.getId(), validatedResultType);
    resultRepository.saveAll(results);
  }

  private List<AnnualExamResult> parseWorkbook(
      MultipartFile file,
      School school,
      Integer totalWorkingDays,
      String resultType,
      ResultPresentationSettings settings) {
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> standardRollCounts = new HashMap<>();
      List<AnnualExamResult> results = new ArrayList<>();

      for (int index = 1; index <= sheet.getLastRowNum(); index++) {
        Row row = sheet.getRow(index);
        if (row == null) {
          continue;
        }
        String standard = cellText(row.getCell(2));
        String name = cellText(row.getCell(3));
        if (standard.isBlank() || name.isBlank()) {
          continue;
        }

        AnnualExamResult result = new AnnualExamResult();
        result.setId(UUID.randomUUID());
        result.setSchoolId(school.getId());
        result.setResultType(resultType);
        result.setStandard(standard);
        result.setRollNumber(standardRollCounts.merge(standard, 1, Integer::sum));
        result.setStudentName(name);
        result.setGeneralRegisterNumber(cellText(row.getCell(1)));
        result.setBirthDate(cellText(row.getCell(4)));
        result.setTotalWorkingDays(totalWorkingDays);
        result.setAttendedDays(optionalInteger(row.getCell(5)));

        addSubjects(result, row, configuredSubjects(school.getId(), standard), sheet.getRow(0));
        calculateTotals(result, settings);
        results.add(result);
      }
      return results;
    } catch (DomainException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }
  }

  private void addSubjects(
      AnnualExamResult result, Row row, List<ConfiguredSubject> configuredSubjects, Row headerRow) {
    Map<Integer, Integer> subjectIndexToColumn =
        resolveSubjectColumns(headerRow, configuredSubjects);

    for (int index = 0; index < configuredSubjects.size(); index++) {
      int marksColumn = subjectIndexToColumn.getOrDefault(index, 6 + index * 2);
      String marks = cellText(row.getCell(marksColumn));
      if (marks.isBlank()) {
        continue;
      }
      AnnualExamResultSubject subject = new AnnualExamResultSubject();
      subject.setId(UUID.randomUUID());
      subject.setSubjectName(configuredSubjects.get(index).name());
      subject.setMaximumMarks(configuredSubjects.get(index).maximumMarks());
      subject.setObtainedMarks(optionalInteger(row.getCell(marksColumn)));
      subject.setGrade(cellText(row.getCell(marksColumn + 1)));
      subject.setSortOrder(index + 1);
      result.addSubject(subject);
    }
    for (int index = configuredSubjects.size(); index < 9; index++) {
      int unconfiguredColumn = 6 + index * 2;
      if (!cellText(row.getCell(unconfiguredColumn)).isBlank()) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
      }
    }
  }

  private Map<Integer, Integer> resolveSubjectColumns(
      Row headerRow, List<ConfiguredSubject> configuredSubjects) {
    Map<Integer, Integer> columnMap = new HashMap<>();
    if (headerRow == null) {
      return columnMap;
    }

    for (int col = 6; col < 30; col += 2) {
      String headerText = cellText(headerRow.getCell(col));
      if (headerText.isBlank()) {
        continue;
      }
      for (int i = 0; i < configuredSubjects.size(); i++) {
        if (!columnMap.containsKey(i)
            && matchesSubjectName(headerText, configuredSubjects.get(i).name())) {
          columnMap.put(i, col);
          break;
        }
      }
    }
    return columnMap;
  }

  private boolean matchesSubjectName(String headerText, String configuredName) {
    if (headerText.equalsIgnoreCase(configuredName)) {
      return true;
    }
    String normHeader = normalizeSubjectString(headerText);
    String normConfig = normalizeSubjectString(configuredName);
    if (!normHeader.isEmpty() && normHeader.equals(normConfig)) {
      return true;
    }

    return isSubjectAliasMatch(normHeader, normConfig);
  }

  private String normalizeSubjectString(String text) {
    if (text == null) {
      return "";
    }
    return text.toLowerCase().replaceAll("[^a-z0-9\\u0900-\\u097F]", "").trim();
  }

  private boolean isSubjectAliasMatch(String a, String b) {
    List<List<String>> aliasGroups =
        List.of(
            List.of("gujarati", "guj", "ગુજરાતી"),
            List.of("mathematics", "maths", "math", "ગણિત"),
            List.of("science", "sci", "વિજ્ઞાન"),
            List.of("hindi", "hin", "હિન્દી"),
            List.of("english", "eng", "અંગ્રેજી"),
            List.of("socialscience", "social", "ss", "સામાજિકવિજ્ઞાન"),
            List.of("sanskrit", "san", "સંસ્કૃત"),
            List.of("personalitydevelopment", "pd", "personality", "વ્યક્તિત્વવિકાસ"),
            List.of("environment", "environmentalstudies", "env", "પર્યાવરણ"));

    for (List<String> group : aliasGroups) {
      if (group.contains(a) && group.contains(b)) {
        return true;
      }
    }
    return false;
  }

  private List<ConfiguredSubject> configuredSubjects(UUID schoolId, String standardValue) {
    Standard standard =
        standards.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId).stream()
            .filter(value -> matchesStandard(value, standardValue))
            .findFirst()
            .orElseThrow(
                () ->
                    new DomainException(HttpStatus.BAD_REQUEST, "result_standard_not_configured"));
    List<ConfiguredSubject> configured =
        standardSubjects
            .findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standard.getId())
            .stream()
            .map(this::configuredSubject)
            .toList();
    if (configured.isEmpty()) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
    }
    return configured;
  }

  private ConfiguredSubject configuredSubject(StandardSubject mapping) {
    if (!mapping.isMaximumMarksConfigured()) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
    }
    String name =
        subjects
            .findByIdAndSchoolId(mapping.getSubjectId(), mapping.getSchoolId())
            .map(value -> value.getName())
            .orElseThrow(
                () ->
                    new DomainException(
                        HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured"));
    return new ConfiguredSubject(name, mapping.getMaximumMarks());
  }

  private record ConfiguredSubject(String name, int maximumMarks) {}

  private boolean matchesStandard(Standard standard, String uploadedValue) {
    if (uploadedValue == null || uploadedValue.isBlank()) {
      return false;
    }
    String rawUploaded = uploadedValue.trim();
    if (rawUploaded.equalsIgnoreCase(standard.getDisplayName())
        || rawUploaded.equalsIgnoreCase(standard.getCode())) {
      return true;
    }

    String asciiUploaded = convertGujaratiDigitsToAscii(rawUploaded);
    String asciiDisplay = convertGujaratiDigitsToAscii(standard.getDisplayName());

    String uploadedNum = extractStandardNum(asciiUploaded);
    String displayNum = extractStandardNum(asciiDisplay);

    return !uploadedNum.isBlank() && uploadedNum.equals(displayNum);
  }

  private String convertGujaratiDigitsToAscii(String text) {
    if (text == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (char c : text.toCharArray()) {
      if (c >= '\u0966' && c <= '\u096F') {
        sb.append((char) ('0' + (c - '\u0966')));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private String extractStandardNum(String text) {
    if (text == null) {
      return "";
    }
    String upper = text.toUpperCase();
    if (upper.contains("VIII") || upper.contains("8")) {
      return "8";
    }
    if (upper.contains("VII") || upper.contains("7")) {
      return "7";
    }
    if (upper.contains("VI") || upper.contains("6")) {
      return "6";
    }
    if (upper.contains("IV") || upper.contains("4")) {
      return "4";
    }
    if (upper.contains("V") || upper.contains("5")) {
      return "5";
    }
    if (upper.contains("III") || upper.contains("3")) {
      return "3";
    }
    if (upper.contains("II") || upper.contains("2")) {
      return "2";
    }
    if (upper.contains("I") || upper.contains("1")) {
      return "1";
    }
    return upper.replaceAll("[^0-9]", "");
  }

  private void calculateTotals(AnnualExamResult result, ResultPresentationSettings settings) {
    int maximumMarks =
        result.getSubjects().stream().mapToInt(AnnualExamResultSubject::getMaximumMarks).sum();
    int obtainedMarks =
        result.getSubjects().stream()
            .map(AnnualExamResultSubject::getObtainedMarks)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum();
    result.setTotalMarks(maximumMarks);
    result.setObtainedMarks(obtainedMarks);
    if (maximumMarks > 0) {
      double percentage = obtainedMarks * 100.0 / maximumMarks;
      result.setPercentage(BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP));
      result.setOverallGrade(calculateGrade(percentage, settings));
    }
  }

  private Integer optionalInteger(Cell cell) {
    String value = cellText(cell);
    if (value.isBlank()) {
      return null;
    }
    try {
      double parsed = Double.parseDouble(value);
      if (parsed != Math.rint(parsed)) {
        throw new NumberFormatException();
      }
      return (int) parsed;
    } catch (NumberFormatException exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }
  }

  private String validateResultType(String resultType) {
    if (ANNUAL.equals(resultType) || EKAM_KASOTI.equals(resultType)) {
      return resultType;
    }
    throw new DomainException(HttpStatus.BAD_REQUEST, "invalid_result_type");
  }

  private School getSchool() {
    return schoolRepository
        .findFirstByIsActiveTrueOrderByCreatedAtAsc()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
  }

  private String cellText(Cell cell) {
    return cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
  }

  private String calculateGrade(double percentage, ResultPresentationSettings settings) {
    if (percentage >= settings.getGradeAMin().doubleValue()) return "A";
    if (percentage >= settings.getGradeBMin().doubleValue()) return "B";
    if (percentage >= settings.getGradeCMin().doubleValue()) return "C";
    if (percentage >= settings.getGradeDMin().doubleValue()) return "D";
    return "E";
  }

  private ExamResultResponse mapToResponse(AnnualExamResult entity) {
    List<ExamResultSubjectResponse> subjects =
        entity.getSubjects().stream()
            .sorted(Comparator.comparingInt(AnnualExamResultSubject::getSortOrder))
            .map(
                subject ->
                    new ExamResultSubjectResponse(
                        subject.getId(),
                        subject.getSubjectName(),
                        subject.getMaximumMarks(),
                        subject.getObtainedMarks(),
                        subject.getGrade(),
                        subject.getSortOrder()))
            .collect(Collectors.toList());
    return new ExamResultResponse(
        entity.getId(),
        entity.getSchoolId(),
        entity.getStudentName(),
        entity.getStandard(),
        entity.getRollNumber(),
        entity.getGeneralRegisterNumber(),
        entity.getBirthDate(),
        entity.getTotalWorkingDays(),
        entity.getAttendedDays(),
        entity.getTotalMarks(),
        entity.getObtainedMarks(),
        entity.getPercentage(),
        entity.getOverallGrade(),
        subjects);
  }
}
