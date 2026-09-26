package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.assessment.api.ExamResultResponse;
import com.rorfost.schoolportal.assessment.api.ExamResultSubjectResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final String ANNUAL = "ANNUAL";
  private static final String EKAM_KASOTI = "EKAM_KASOTI";
  private static final List<String> EKAM_HEADERS =
      List.of(
          "AADHAARUID",
          "STUDENTNAME",
          "ATTENDANCE",
          "GUJARATI(40)",
          "MATHS(40)",
          "EVS(40)",
          "ENGLISH(40)",
          "TOTAL(160)");
  private static final Pattern SUBJECT_HEADER = Pattern.compile("(.+?)\\s*\\((\\d+)\\)");
  private static final Pattern STANDARD_IN_TITLE =
      Pattern.compile(
          "(?:ધોરણ|standard|std)\\s*(?:no\\.?\\s*)?[-:]?\\s*([1-8])",
          Pattern.CASE_INSENSITIVE);

  private final AnnualExamResultRepository resultRepository;
  private final SchoolRepository schoolRepository;
  private final AcademicYearRepository academicYearRepository;
  private final StandardRepository standardRepository;
  private final StudentRepository studentRepository;
  private final DataFormatter dataFormatter = new DataFormatter();

  public ExamResultService(
      AnnualExamResultRepository resultRepository,
      SchoolRepository schoolRepository,
      AcademicYearRepository academicYearRepository,
      StandardRepository standardRepository,
      StudentRepository studentRepository) {
    this.resultRepository = resultRepository;
    this.schoolRepository = schoolRepository;
    this.academicYearRepository = academicYearRepository;
    this.standardRepository = standardRepository;
    this.studentRepository = studentRepository;
  }

  @Transactional(readOnly = true)
  public ExamResultResponse getResult(String standard, Integer rollNumber) {
    School school = getSchool();
    AcademicYear academicYear = getAcademicYear(school.getId());
    return resultRepository
        .findBySchoolIdAndAcademicYearIdAndResultTypeAndStandardAndRollNumber(
            school.getId(), academicYear.getId(), ANNUAL, standard, rollNumber)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
  }

  @Transactional
  public void processAnnualExcelUpload(MultipartFile file, Integer totalWorkingDays) {
    if (totalWorkingDays == null || totalWorkingDays < 1) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "validation_failed");
    }
    School school = getSchool();
    AcademicYear academicYear = getAcademicYear(school.getId());
    List<AnnualExamResult> results = parseAnnualWorkbook(file, school, academicYear, totalWorkingDays);
    if (results.isEmpty()) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "annual_exam_format_invalid");
    }
    resultRepository.deleteBySchoolIdAndAcademicYearIdAndResultType(
        school.getId(), academicYear.getId(), ANNUAL);
    resultRepository.saveAll(results);
  }

  /**
   * Aadhaar is deliberately never read, logged, or stored. Each workbook row is matched only to
   * an already enrolled student with a school-controlled roll number.
   */
  @Transactional
  public void processEkamKasotiUpload(MultipartFile file) {
    requireXlsx(file);
    School school = getSchool();
    AcademicYear academicYear = getAcademicYear(school.getId());
    List<AnnualExamResult> results;
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      if (workbook.getNumberOfSheets() != 1) {
        throw invalidEkamFormat();
      }
      results = parseEkamKasotiWorkbook(workbook.getSheetAt(0), school, academicYear);
    } catch (DomainException exception) {
      throw exception;
    } catch (Exception exception) {
      throw invalidEkamFormat();
    }
    resultRepository.deleteBySchoolIdAndAcademicYearIdAndResultType(
        school.getId(), academicYear.getId(), EKAM_KASOTI);
    resultRepository.saveAll(results);
  }

  private List<AnnualExamResult> parseAnnualWorkbook(
      MultipartFile file, School school, AcademicYear academicYear, Integer totalWorkingDays) {
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> standardRollCounts = new HashMap<>();
      List<AnnualExamResult> results = new ArrayList<>();
      for (int index = 1; index <= sheet.getLastRowNum(); index++) {
        Row row = sheet.getRow(index);
        if (row == null) continue;
        String standard = cellText(row.getCell(2));
        String name = cellText(row.getCell(3));
        if (standard.isBlank() || name.isBlank()) continue;

        AnnualExamResult result =
            baseResult(
                school,
                academicYear,
                ANNUAL,
                standard,
                standardRollCounts.merge(standard, 1, Integer::sum),
                name);
        result.setGeneralRegisterNumber(cellText(row.getCell(1)));
        result.setBirthDate(cellText(row.getCell(4)));
        result.setTotalWorkingDays(totalWorkingDays);
        result.setAttendedDays(optionalInteger(row.getCell(5), "annual_exam_format_invalid"));
        addAnnualSubjects(result, row);
        calculateAnnualTotals(result);
        results.add(result);
      }
      return results;
    } catch (DomainException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "annual_exam_format_invalid");
    }
  }

  private List<AnnualExamResult> parseEkamKasotiWorkbook(
      Sheet sheet, School school, AcademicYear academicYear) {
    String standardCode = standardFromTitle(cellText(requiredRow(sheet, 0).getCell(0)));
    validateEkamHeaders(requiredRow(sheet, 1));
    Standard standard =
        standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(school.getId()).stream()
            .filter(candidate -> isMatchingStandard(candidate, standardCode))
            .findFirst()
            .orElseThrow(this::invalidEkamFormat);
    Map<String, Student> studentsByName = studentsByName(school.getId(), academicYear.getId(), standard);

    List<AnnualExamResult> results = new ArrayList<>();
    Set<UUID> importedStudentIds = new HashSet<>();
    for (int index = 2; index <= sheet.getLastRowNum(); index++) {
      Row row = sheet.getRow(index);
      if (row == null) continue;
      String workbookName = cellText(row.getCell(1));
      if (workbookName.isBlank()) continue;
      Student student = studentsByName.get(normalizeName(workbookName));
      if (student == null || !importedStudentIds.add(student.getId())) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "ekam_kasoti_students_not_enrolled");
      }

      AnnualExamResult result =
          baseResult(
              school,
              academicYear,
              EKAM_KASOTI,
              standardCode,
              parseRollNumber(student.getRollNumber()),
              student.getFullName());
      int totalMaximumMarks = 0;
      int totalObtainedMarks = 0;
      for (int column = 3; column <= 6; column++) {
        Matcher header = SUBJECT_HEADER.matcher(cellText(sheet.getRow(1).getCell(column)));
        if (!header.matches()) throw invalidEkamFormat();
        int maximumMarks = Integer.parseInt(header.group(2));
        Integer obtainedMarks = optionalEkamMarks(row.getCell(column), maximumMarks);
        addSubject(result, header.group(1).trim(), maximumMarks, obtainedMarks, column - 2);
        totalMaximumMarks += maximumMarks;
        totalObtainedMarks += obtainedMarks == null ? 0 : obtainedMarks;
      }
      result.setTotalMarks(totalMaximumMarks);
      result.setObtainedMarks(totalObtainedMarks);
      result.setPercentage(
          BigDecimal.valueOf(totalObtainedMarks * 100.0 / totalMaximumMarks)
              .setScale(2, RoundingMode.HALF_UP));
      results.add(result);
    }
    if (results.isEmpty()) throw invalidEkamFormat();
    return results;
  }

  private Map<String, Student> studentsByName(UUID schoolId, UUID academicYearId, Standard standard) {
    Map<String, Student> students = new HashMap<>();
    for (Student student :
        studentRepository.findBySchoolIdAndAcademicYearIdAndStandardId(
            schoolId, academicYearId, standard.getId())) {
      if (student.isArchived()) continue;
      if (students.put(normalizeName(student.getFullName()), student) != null) {
        throw new DomainException(HttpStatus.BAD_REQUEST, "ekam_kasoti_duplicate_student_name");
      }
    }
    return students;
  }

  private AnnualExamResult baseResult(
      School school,
      AcademicYear academicYear,
      String resultType,
      String standard,
      Integer rollNumber,
      String studentName) {
    AnnualExamResult result = new AnnualExamResult();
    result.setId(UUID.randomUUID());
    result.setSchoolId(school.getId());
    result.setAcademicYearId(academicYear.getId());
    result.setResultType(resultType);
    result.setStandard(standard);
    result.setRollNumber(rollNumber);
    result.setStudentName(studentName);
    return result;
  }

  private void addAnnualSubjects(AnnualExamResult result, Row row) {
    String[] names = {"ગુજરાતી", "ગણિત", "વિજ્ઞાન", "હિન્દી", "અંગ્રેજી", "સામાજિક વિજ્ઞાન", "સંસ્કૃત", "વ્યક્તિત્વ વિકાસ", "પર્યાવરણ"};
    int[] maximums = {200, 200, 200, 200, 200, 200, 200, 400, 200};
    for (int index = 0; index < names.length; index++) {
      int marksColumn = 6 + index * 2;
      String marks = cellText(row.getCell(marksColumn));
      if (marks.isBlank()) continue;
      addSubject(
          result,
          names[index],
          maximums[index],
          optionalInteger(row.getCell(marksColumn), "annual_exam_format_invalid"),
          index + 1);
      result.getSubjects().getLast().setGrade(cellText(row.getCell(marksColumn + 1)));
    }
  }

  private void addSubject(
      AnnualExamResult result,
      String name,
      int maximumMarks,
      Integer obtainedMarks,
      int sortOrder) {
    AnnualExamResultSubject subject = new AnnualExamResultSubject();
    subject.setId(UUID.randomUUID());
    subject.setSubjectName(name);
    subject.setMaximumMarks(maximumMarks);
    subject.setObtainedMarks(obtainedMarks);
    subject.setSortOrder(sortOrder);
    result.addSubject(subject);
  }

  private void calculateAnnualTotals(AnnualExamResult result) {
    int maximumMarks = result.getSubjects().stream().mapToInt(AnnualExamResultSubject::getMaximumMarks).sum();
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
      result.setOverallGrade(calculateGrade(percentage));
    }
  }

  private void validateEkamHeaders(Row row) {
    for (int column = 0; column < EKAM_HEADERS.size(); column++) {
      if (!normalizeHeader(cellText(row.getCell(column))).equals(EKAM_HEADERS.get(column))) {
        throw invalidEkamFormat();
      }
    }
  }

  private String standardFromTitle(String title) {
    Matcher matcher = STANDARD_IN_TITLE.matcher(toAsciiDigits(title));
    if (!matcher.find()) throw invalidEkamFormat();
    return matcher.group(1);
  }

  private boolean isMatchingStandard(Standard standard, String standardCode) {
    return standard.getCode().equals(standardCode)
        || Pattern.compile("(^|\\D)" + Pattern.quote(standardCode) + "(\\D|$)")
            .matcher(toAsciiDigits(standard.getDisplayName()))
            .find();
  }

  private Integer optionalEkamMarks(Cell cell, int maximumMarks) {
    String value = cellText(cell);
    if (value.equals("-")) return null;
    Integer marks = optionalInteger(cell, "ekam_kasoti_format_invalid");
    if (marks == null || marks < 0 || marks > maximumMarks) throw invalidEkamFormat();
    return marks;
  }

  private Integer optionalInteger(Cell cell, String code) {
    String value = cellText(cell);
    if (value.isBlank()) return null;
    try {
      double parsed = Double.parseDouble(value);
      if (parsed != Math.rint(parsed)) throw new NumberFormatException();
      return (int) parsed;
    } catch (NumberFormatException exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, code);
    }
  }

  private Integer parseRollNumber(String rollNumber) {
    try {
      int parsed = Integer.parseInt(rollNumber);
      if (parsed < 1) throw new NumberFormatException();
      return parsed;
    } catch (NumberFormatException exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "ekam_kasoti_students_not_enrolled");
    }
  }

  private School getSchool() {
    return schoolRepository
        .findFirstByIsActiveTrueOrderByCreatedAtAsc()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
  }

  private AcademicYear getAcademicYear(UUID schoolId) {
    return academicYearRepository.findBySchoolIdOrderByStartsOnDesc(schoolId).stream()
        .filter(year -> year.getStatus() == AcademicYearStatus.CURRENT)
        .findFirst()
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "academic_year_not_found"));
  }

  private Row requiredRow(Sheet sheet, int index) {
    Row row = sheet.getRow(index);
    if (row == null) throw invalidEkamFormat();
    return row;
  }

  private void requireXlsx(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (file.isEmpty() || filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
      throw invalidEkamFormat();
    }
  }

  private DomainException invalidEkamFormat() {
    return new DomainException(HttpStatus.BAD_REQUEST, "ekam_kasoti_format_invalid");
  }

  private String cellText(Cell cell) {
    return cell == null ? "" : dataFormatter.formatCellValue(cell).trim();
  }

  private String normalizeHeader(String value) {
    return value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
  }

  private String normalizeName(String value) {
    return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private String toAsciiDigits(String value) {
    return value
        .replace('૦', '0')
        .replace('૧', '1')
        .replace('૨', '2')
        .replace('૩', '3')
        .replace('૪', '4')
        .replace('૫', '5')
        .replace('૬', '6')
        .replace('૭', '7')
        .replace('૮', '8')
        .replace('૯', '9');
  }

  private String calculateGrade(double percentage) {
    if (percentage >= 80) return "A";
    if (percentage >= 65) return "B";
    if (percentage >= 50) return "C";
    if (percentage >= 35) return "D";
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
        entity.getAcademicYearId(),
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
