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

        addSubjects(result, row, configuredSubjects(school.getId(), standard));
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
      AnnualExamResult result, Row row, List<ConfiguredSubject> configuredSubjects) {
    for (int index = 0; index < configuredSubjects.size(); index++) {
      int marksColumn = 6 + index * 2;
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
      if (!cellText(row.getCell(6 + index * 2)).isBlank())
        throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
    }
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
    if (configured.isEmpty())
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
    return configured;
  }

  private ConfiguredSubject configuredSubject(StandardSubject mapping) {
    if (!mapping.isMaximumMarksConfigured())
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_maximums_not_configured");
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
    if (uploadedValue.equalsIgnoreCase(standard.getDisplayName())
        || uploadedValue.equalsIgnoreCase(standard.getCode())) return true;
    String uploadedNumber = uploadedValue.replaceAll("[^0-9]", "");
    String displayNumber = standard.getDisplayName().replaceAll("[^0-9]", "");
    return !uploadedNumber.isBlank() && uploadedNumber.equals(displayNumber);
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
