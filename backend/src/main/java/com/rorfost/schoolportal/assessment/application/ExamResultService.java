package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.assessment.api.ExamResultResponse;
import com.rorfost.schoolportal.assessment.api.ExamResultSubjectResponse;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultRepository;
import com.rorfost.schoolportal.school.domain.AnnualExamResultSubject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExamResultService {

  private final AnnualExamResultRepository resultRepository;
  private final SchoolRepository schoolRepository;
  private final AcademicYearRepository academicYearRepository;

  public ExamResultService(
      AnnualExamResultRepository resultRepository,
      SchoolRepository schoolRepository,
      AcademicYearRepository academicYearRepository) {
    this.resultRepository = resultRepository;
    this.schoolRepository = schoolRepository;
    this.academicYearRepository = academicYearRepository;
  }

  private School getSchool() {
      return schoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
  }

  private AcademicYear getAcademicYear(UUID schoolId) {
      return academicYearRepository.findBySchoolIdOrderByStartsOnDesc(schoolId).stream()
          .filter(y -> y.getStatus() == AcademicYearStatus.CURRENT)
          .findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));
  }

  @Transactional(readOnly = true)
  public ExamResultResponse getResult(String standard, Integer rollNumber) {
    var school = getSchool();
    var academicYear = getAcademicYear(school.getId());

    return resultRepository
        .findBySchoolIdAndAcademicYearIdAndStandardAndRollNumber(
            school.getId(), academicYear.getId(), standard, rollNumber)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
  }

  @Transactional
  public void processExcelUpload(MultipartFile file, Integer totalWorkingDays) {
    var school = getSchool();
    var academicYear = getAcademicYear(school.getId());

    resultRepository.deleteBySchoolIdAndAcademicYearId(school.getId(), academicYear.getId());

    try (InputStream is = file.getInputStream();
         Workbook workbook = WorkbookFactory.create(is)) {

      Sheet sheet = workbook.getSheetAt(0);
      Map<String, Integer> standardRollCounts = new HashMap<>();
      List<AnnualExamResult> results = new ArrayList<>();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        String standard = getCellString(row.getCell(2));
        String name = getCellString(row.getCell(3));

        if (standard == null || standard.isBlank() || name == null || name.isBlank()) {
          continue; // skip empty or invalid rows
        }

        int roll = standardRollCounts.getOrDefault(standard, 0) + 1;
        standardRollCounts.put(standard, roll);

        AnnualExamResult result = new AnnualExamResult();
        result.setId(UUID.randomUUID());
        result.setSchoolId(school.getId());
        result.setAcademicYearId(academicYear.getId());
        result.setStandard(standard);
        result.setRollNumber(roll);
        result.setStudentName(name);
        result.setGeneralRegisterNumber(getCellString(row.getCell(1)));
        result.setBirthDate(getCellString(row.getCell(4)));
        result.setTotalWorkingDays(totalWorkingDays);
        
        String hajar = getCellString(row.getCell(5));
        if (hajar != null && !hajar.isBlank()) {
          try {
             result.setAttendedDays((int) Double.parseDouble(hajar));
          } catch(Exception ignored) {}
        }

        int totalObtained = 0;
        int totalMax = 0;
        int sortOrder = 1;

        // Gujarati
        totalMax += addSubject(result, "ગુજરાતી", 200, row.getCell(6), row.getCell(7), sortOrder++);
        // Math
        totalMax += addSubject(result, "ગણિત", 200, row.getCell(8), row.getCell(9), sortOrder++);
        // Science
        totalMax += addSubject(result, "વિજ્ઞાન", 200, row.getCell(10), row.getCell(11), sortOrder++);
        // Hindi
        totalMax += addSubject(result, "હિન્દી", 200, row.getCell(12), row.getCell(13), sortOrder++);
        // English
        totalMax += addSubject(result, "અંગ્રેજી", 200, row.getCell(14), row.getCell(15), sortOrder++);
        // SS
        totalMax += addSubject(result, "સામાજીક વિજ્ઞાન", 200, row.getCell(16), row.getCell(17), sortOrder++);
        // Sanskrit
        totalMax += addSubject(result, "સંસ્કૃત", 200, row.getCell(18), row.getCell(19), sortOrder++);
        // VV
        totalMax += addSubject(result, "વ્યક્તિત્વ વિકાસ", 400, row.getCell(20), row.getCell(21), sortOrder++);
        // Paryavaran
        totalMax += addSubject(result, "પર્યાવરણ", 200, row.getCell(22), row.getCell(23), sortOrder++);

        for (AnnualExamResultSubject sub : result.getSubjects()) {
           if (sub.getObtainedMarks() != null) {
               totalObtained += sub.getObtainedMarks();
           }
        }

        result.setTotalMarks(totalMax);
        result.setObtainedMarks(totalObtained);
        
        if (totalMax > 0) {
            double p = (totalObtained * 100.0) / totalMax;
            result.setPercentage(BigDecimal.valueOf(p).setScale(2, java.math.RoundingMode.HALF_UP));
            result.setOverallGrade(calculateGrade(p));
        }

        results.add(result);
      }
      
      resultRepository.saveAll(results);

    } catch (Exception e) {
      throw new RuntimeException("Failed to parse Excel file", e);
    }
  }

  private String calculateGrade(double p) {
    if (p >= 80) return "A";
    if (p >= 65) return "B";
    if (p >= 50) return "C";
    if (p >= 35) return "D";
    return "E";
  }

  private int addSubject(AnnualExamResult result, String name, int max, Cell marksCell, Cell gradeCell, int sortOrder) {
      String marks = getCellString(marksCell);
      String grade = getCellString(gradeCell);
      if (marks == null || marks.isBlank()) return 0; // Skip subject if marks are empty (e.g. Std 3 no English)
      
      AnnualExamResultSubject sub = new AnnualExamResultSubject();
      sub.setId(UUID.randomUUID());
      sub.setSubjectName(name);
      sub.setMaximumMarks(max);
      sub.setSortOrder(sortOrder);
      sub.setGrade(grade);
      try {
          sub.setObtainedMarks((int) Double.parseDouble(marks));
      } catch(Exception ignored) {}
      
      result.addSubject(sub);
      return max;
  }

  private String getCellString(Cell cell) {
    if (cell == null) return null;
    DataFormatter df = new DataFormatter();
    return df.formatCellValue(cell).trim();
  }

  private ExamResultResponse mapToResponse(AnnualExamResult entity) {
    List<ExamResultSubjectResponse> subjects = entity.getSubjects().stream()
        .sorted(Comparator.comparingInt(AnnualExamResultSubject::getSortOrder))
        .map(s -> new ExamResultSubjectResponse(
            s.getId(), s.getSubjectName(), s.getMaximumMarks(), s.getObtainedMarks(), s.getGrade(), s.getSortOrder()
        )).collect(Collectors.toList());

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
        subjects
    );
  }
}
