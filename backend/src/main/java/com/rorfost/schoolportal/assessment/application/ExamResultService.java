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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
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
    return getResult(standard, null, rollNumber, resultType);
  }

  @Transactional(readOnly = true)
  public ExamResultResponse getResult(
      String standard, String studentClass, Integer rollNumber, String resultType) {
    School school = getSchool();
    String trimmedClass =
        (studentClass == null || studentClass.isBlank()) ? null : studentClass.trim();
    return resultRepository
        .findResult(
            school.getId(),
            validateResultType(resultType),
            standard.trim(),
            trimmedClass,
            rollNumber)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
  }

  @Transactional
  public void processExcelUpload(MultipartFile file, String totalWorkingDays, String resultType) {
    String validatedResultType = validateResultType(resultType);

    School school = getSchool();
    ResultPresentationSettings settings =
        presentationSettings
            .findById(school.getId())
            .orElseGet(() -> new ResultPresentationSettings(school.getId()));
    String persistedWorkingDays =
        (totalWorkingDays != null && !totalWorkingDays.isBlank()) ? totalWorkingDays.trim() : null;
    List<AnnualExamResult> results =
        parseWorkbook(file, school, persistedWorkingDays, validatedResultType, settings);
    if (results.isEmpty()) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }

    resultRepository.deleteBySchoolIdAndResultType(school.getId(), validatedResultType);
    resultRepository.saveAll(results);
  }

  @Transactional
  public void clearResults(String resultType) {
    String validatedResultType = validateResultType(resultType);
    School school = getSchool();

    resultRepository.deleteBySchoolIdAndResultType(school.getId(), validatedResultType);
  }

  private List<AnnualExamResult> parseWorkbook(
      MultipartFile file,
      School school,
      String totalWorkingDays,
      String resultType,
      ResultPresentationSettings settings) {
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> standardRollCounts = new HashMap<>();
      List<AnnualExamResult> results = new ArrayList<>();

      boolean isEkam = EKAM_KASOTI.equals(resultType);
      // Fixed columns: GR=0, UID=1, Std=2, Class=3, Name=4, BirthDate=5
      int colGr = 0;
      int colStudentUid = 1;
      int colStd = 2;
      int colClass = 3;
      int colName = 4;
      int colBirth = 5;

      // Auto-detect where subjects begin by finding the first known subject header.
      // Col 6 is either "Present Divas" (attended days) or the first subject.
      int colSubjectsStart = detectSubjectsStartColumn(sheet.getRow(0));
      // The attended-days column is the one just before subjects start (if col > 6).
      int colAttended = (colSubjectsStart > 6) ? colSubjectsStart - 1 : -1;

      int subjectColumnStep = detectSubjectColumnStep(sheet.getRow(0), colSubjectsStart);

      // Cache configured subjects and resolved header column mappings per standard
      // to avoid N+1 database queries and 200+ redundant header scans.
      Map<String, StandardSubjectContext> subjectContextCache = new HashMap<>();
      Row headerRow = sheet.getRow(0);

      for (int index = 1; index <= sheet.getLastRowNum(); index++) {
        Row row = sheet.getRow(index);
        if (row == null) {
          continue;
        }
        String standard = cellText(row.getCell(colStd));
        String name = cellText(row.getCell(colName));
        if (standard.isBlank() || name.isBlank()) {
          continue;
        }

        String studentClass = cellText(row.getCell(colClass));
        String rollKey =
            standard
                + (studentClass.isBlank()
                    ? ""
                    : "#" + studentClass.trim().toUpperCase(Locale.ROOT));

        AnnualExamResult result = new AnnualExamResult();
        result.setId(UUID.randomUUID());
        result.setSchoolId(school.getId());
        result.setResultType(resultType);
        result.setStandard(standard);
        result.setStudentClass(studentClass.isBlank() ? null : studentClass.trim());
        result.setRollNumber(standardRollCounts.merge(rollKey, 1, Integer::sum));
        result.setStudentUid(cellText(row.getCell(colStudentUid)));
        result.setStudentName(name);
        result.setGeneralRegisterNumber(cellText(row.getCell(colGr)));
        result.setBirthDate(normalizeBirthDate(row.getCell(colBirth)));
        result.setTotalWorkingDays(totalWorkingDays);

        String attendedValue = colAttended >= 0 ? cellText(row.getCell(colAttended)) : null;
        result.setAttendedDays(
            attendedValue != null && !attendedValue.isBlank() ? attendedValue : null);

        // Reuse cached subject context — avoids DB lookups and header resolution for every row.
        StandardSubjectContext context =
            subjectContextCache.computeIfAbsent(
                standard,
                s -> {
                  List<ConfiguredSubject> configured = configuredSubjects(school.getId(), s);
                  Map<Integer, Integer> indexToCol =
                      resolveSubjectColumns(
                          headerRow, configured, colSubjectsStart, subjectColumnStep);
                  Set<Integer> matchedCols = new HashSet<>(indexToCol.values());
                  return new StandardSubjectContext(configured, indexToCol, matchedCols);
                });

        addSubjects(
            result,
            row,
            context,
            headerRow,
            colSubjectsStart,
            subjectColumnStep,
            !isEkam,
            settings);
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
      AnnualExamResult result,
      Row row,
      StandardSubjectContext context,
      Row headerRow,
      int subjectsStartCol,
      int subjectColumnStep,
      boolean isAnnual,
      ResultPresentationSettings settings) {

    List<ConfiguredSubject> configuredSubjects = context.configuredSubjects();
    Map<Integer, Integer> subjectIndexToColumn = context.subjectIndexToColumn();

    for (int index = 0; index < configuredSubjects.size(); index++) {

      Integer subjectStartColumn = subjectIndexToColumn.get(index);

      if (subjectStartColumn == null) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_header_invalid");
      }

      int maximumMarksColumn = subjectStartColumn;
      int obtainedMarksColumn = subjectStartColumn + 1;

      String maxMarksText = cellText(row.getCell(maximumMarksColumn));
      if (maxMarksText.isBlank()) {
        continue;
      }

      Integer maximumMarks = optionalInteger(row.getCell(maximumMarksColumn));
      if (maximumMarks == null || maximumMarks <= 0) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
      }

      String obtainedValue = cellText(row.getCell(obtainedMarksColumn));

      AnnualExamResultSubject subject = new AnnualExamResultSubject();
      subject.setId(UUID.randomUUID());
      subject.setSubjectName(configuredSubjects.get(index).name());
      subject.setMaximumMarks(maximumMarks);
      subject.setSortOrder(index + 1);

      if (isAbsentValue(obtainedValue)) {
        // Explicitly marked absent (AB / ABS / ABSENT / ગેરહાજર) → show "AB" on result.
        subject.setStatus("ABSENT");
        subject.setObtainedMarks(null);
        subject.setGrade(null);
      } else if (obtainedValue.isBlank()) {
        // Obtained marks cell is empty → marks not filled in; show "-" on result (not "AB").
        subject.setStatus("PRESENT");
        subject.setObtainedMarks(null);
        subject.setGrade(null);
      } else {
        int obtainedMarks = requiredNonNegativeInteger(row.getCell(obtainedMarksColumn));

        if (obtainedMarks > maximumMarks) {
          throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
        }

        subject.setStatus("PRESENT");
        subject.setObtainedMarks(obtainedMarks);

        if (isAnnual) {
          double pct = (obtainedMarks * 100.0) / maximumMarks;
          subject.setGrade(calculateGrade(pct, settings));
        } else {
          subject.setGrade(null);
        }
      }

      result.addSubject(subject);
    }

    // Check if a subject not configured for this standard has obtained-marks data.
    // We only check the OBTAINED column (offset 1), NOT the max-marks column (offset 0).
    Set<Integer> matchedSubjectColumns = context.matchedSubjectColumns();

    int lastColumn = headerRow == null ? subjectsStartCol : headerRow.getLastCellNum();

    for (int col = subjectsStartCol; col < lastColumn; col += subjectColumnStep) {

      // Subject is configured for this standard — already processed above.
      if (matchedSubjectColumns.contains(col)) {
        continue;
      }

      // Only flag an error if the student has OBTAINED MARKS data for an unconfigured subject.
      // An unconfigured subject with only max marks filled (and blank obtained marks) is fine.
      int obtainedCol = col + 1;
      boolean hasObtainedData = !cellText(row.getCell(obtainedCol)).isBlank();

      if (hasObtainedData) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "result_subjects_not_configured");
      }
    }
  }

  private int requiredPositiveInteger(Cell cell) {
    Integer value = optionalInteger(cell);

    if (value == null || value <= 0) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }

    return value;
  }

  private int requiredNonNegativeInteger(Cell cell) {
    Integer value = optionalInteger(cell);

    if (value == null || value < 0) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
    }

    return value;
  }

  private boolean isAbsentValue(String value) {
    if (value == null) {
      return false;
    }

    String normalized = value.trim().toUpperCase(Locale.ROOT);

    return normalized.equals("AB")
        || normalized.equals("ABS")
        || normalized.equals("ABSENT")
        || value.trim().equals("ગેરહાજર");
  }

  private Map<Integer, Integer> resolveSubjectColumns(
      Row headerRow,
      List<ConfiguredSubject> configuredSubjects,
      int subjectsStartCol,
      int subjectColumnStep) {
    Map<Integer, Integer> columnMap = new HashMap<>();
    if (headerRow == null) {
      return columnMap;
    }

    int lastColumn = headerRow.getLastCellNum();

    for (int col = subjectsStartCol; col < lastColumn; col += subjectColumnStep) {

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

    return text.toLowerCase().replaceAll("[^a-z0-9\\u0900-\\u097F\\u0A80-\\u0AFF]", "").trim();
  }

  private static final List<List<String>> ALIAS_GROUPS =
      List.of(
          List.of("gujarati", "guj", "ગુજરાતી"),
          List.of("mathematics", "maths", "math", "ગણિત"),
          List.of("science", "sci", "વિજ્ઞાન"),
          List.of("hindi", "hin", "હિન્દી"),
          List.of("english", "eng", "અંગ્રેજી"),
          List.of("socialscience", "social", "ss", "સામાજિકવિજ્ઞાન"),
          List.of("sanskrit", "san", "સંસ્કૃત"),
          List.of("personalitydevelopment", "pd", "personality", "vv", "વ્યક્તિત્વવિકાસ"),
          List.of("environment", "environmentalstudies", "env", "evs", "પર્યાવરણ"));

  private record StandardSubjectContext(
      List<ConfiguredSubject> configuredSubjects,
      Map<Integer, Integer> subjectIndexToColumn,
      Set<Integer> matchedSubjectColumns) {}

  private boolean isSubjectAliasMatch(String a, String b) {
    for (List<String> group : ALIAS_GROUPS) {
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
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_subject_not_configured");
    }
    return configured;
  }

  private ConfiguredSubject configuredSubject(StandardSubject mapping) {

    String name =
        subjects
            .findByIdAndSchoolId(mapping.getSubjectId(), mapping.getSchoolId())
            .map(value -> value.getName())
            .orElseThrow(
                () ->
                    new DomainException(HttpStatus.BAD_REQUEST, "result_subjects_not_configured"));

    return new ConfiguredSubject(name);
  }

  private record ConfiguredSubject(String name) {}

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
      if (c >= '\u0AE6' && c <= '\u0AEF') {
        sb.append((char) ('0' + (c - '\u0AE6')));
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
      if (!EKAM_KASOTI.equals(result.getResultType())) {
        result.setOverallGrade(calculateGrade(percentage, settings));
      }
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
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
      double val = cell.getNumericCellValue();
      if (val == Math.rint(val)) {
        return String.valueOf((long) val);
      }
    }
    return dataFormatter.formatCellValue(cell).trim();
  }

  /**
   * Scans the header row starting from column 6 to find the first column whose header is a known
   * subject name. Returns that column index as the subjects-start column. Falls back to 7 if no
   * known subject is found (safe default for files with an attended-days column).
   */
  private int detectSubjectsStartColumn(Row headerRow) {
    if (headerRow == null) {
      return 7;
    }
    int lastColumn = headerRow.getLastCellNum();
    // The earliest a subject column can appear is index 6 (no attended-days col).
    for (int col = 6; col < lastColumn; col++) {
      String text = cellText(headerRow.getCell(col));
      if (!text.isBlank() && isKnownSubjectName(text)) {
        return col;
      }
    }
    // Fallback: assume attended-days is at 6, subjects start at 7.
    return 7;
  }

  private int detectSubjectColumnStep(Row headerRow, int subjectsStartCol) {
    if (headerRow == null) {
      return 2;
    }
    int lastColumn = headerRow.getLastCellNum();
    List<Integer> matchedCols = new ArrayList<>();
    for (int col = subjectsStartCol; col < lastColumn; col++) {
      String text = cellText(headerRow.getCell(col));
      if (!text.isBlank() && isKnownSubjectName(text)) {
        matchedCols.add(col);
      }
    }
    if (matchedCols.size() >= 2) {
      int step = matchedCols.get(1) - matchedCols.get(0);
      if (step == 2 || step == 3) {
        return step;
      }
    }
    return 2;
  }

  private boolean isKnownSubjectName(String text) {
    String norm = normalizeSubjectString(text);
    if (norm.isEmpty()) return false;
    List<String> known =
        List.of(
            "gujarati",
            "guj",
            "ગુજરાતી",
            "mathematics",
            "maths",
            "math",
            "ગણિત",
            "science",
            "sci",
            "વિજ્ઞાન",
            "hindi",
            "hin",
            "હિન્દી",
            "english",
            "eng",
            "અંગ્રેજી",
            "socialscience",
            "social",
            "ss",
            "સામાજિકવિજ્ઞાન",
            "sanskrit",
            "san",
            "સંસ્કૃત",
            "personalitydevelopment",
            "pd",
            "personality",
            "vv",
            "વ્યક્તિત્વવિકાસ",
            "environment",
            "environmentalstudies",
            "env",
            "evs",
            "પર્યાવરણ");
    return known.contains(norm);
  }

  private String normalizeBirthDate(Cell cell) {
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      try {
        java.time.LocalDateTime ldt = cell.getLocalDateTimeCellValue();
        if (ldt != null) {
          return String.format(
              Locale.ROOT,
              "%02d/%02d/%04d",
              ldt.getDayOfMonth(),
              ldt.getMonthValue(),
              ldt.getYear());
        }
      } catch (Exception ignored) {
      }
    }
    String text = cellText(cell);
    if (text.isBlank()) {
      return "";
    }
    String[] parts = text.split("[^0-9]+");
    if (parts.length == 3) {
      String p1 = parts[0];
      String p2 = parts[1];
      String p3 = parts[2];
      try {
        if (p1.length() == 4) {
          return String.format(
              Locale.ROOT, "%02d/%02d/%s", Integer.parseInt(p3), Integer.parseInt(p2), p1);
        } else {
          int yearVal = Integer.parseInt(p3);
          if (p3.length() <= 2) {
            yearVal = yearVal > 50 ? 1900 + yearVal : 2000 + yearVal;
          }
          return String.format(
              Locale.ROOT, "%02d/%02d/%04d", Integer.parseInt(p1), Integer.parseInt(p2), yearVal);
        }
      } catch (NumberFormatException ignored) {
      }
    }
    return text;
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
                        subject.getStatus(),
                        subject.getSortOrder()))
            .collect(Collectors.toList());
    return new ExamResultResponse(
        entity.getId(),
        entity.getSchoolId(),
        entity.getStudentUid(),
        entity.getStudentName(),
        entity.getStandard(),
        entity.getStudentClass(),
        entity.getRollNumber(),
        entity.getGeneralRegisterNumber(),
        entity.getBirthDate(),
        (entity.getTotalWorkingDays() == null || entity.getTotalWorkingDays().isBlank())
            ? "-"
            : entity.getTotalWorkingDays(),
        (entity.getAttendedDays() == null || entity.getAttendedDays().isBlank())
            ? "-"
            : entity.getAttendedDays(),
        entity.getTotalMarks(),
        entity.getObtainedMarks(),
        entity.getPercentage(),
        entity.getOverallGrade(),
        subjects);
  }
}
