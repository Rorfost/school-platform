package com.rorfost.schoolportal.assessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

class ExamResultServiceTest {
  private final AnnualExamResultRepository results = Mockito.mock(AnnualExamResultRepository.class);
  private final SchoolRepository schools = Mockito.mock(SchoolRepository.class);
  private final ResultPresentationSettingsRepository presentationSettings =
      Mockito.mock(ResultPresentationSettingsRepository.class);
  private final StandardRepository standards = Mockito.mock(StandardRepository.class);
  private final StandardSubjectRepository standardSubjects =
      Mockito.mock(StandardSubjectRepository.class);
  private final SubjectRepository subjects = Mockito.mock(SubjectRepository.class);
  private final ExamResultService service =
      new ExamResultService(
          results, schools, presentationSettings, standards, standardSubjects, subjects);

  @Test
  void importsEkamKasotiWorkbookWithoutHajarDivas() throws IOException {
    UUID schoolId = UUID.randomUUID();
    School school = Mockito.mock(School.class);
    when(school.getId()).thenReturn(schoolId);
    when(schools.findFirstByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(school));
    Standard standard = new Standard(schoolId, "8", "8", (short) 8);
    UUID standardId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(standard, "id", standardId);
    when(standards.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId))
        .thenReturn(List.of(standard));
    List<StandardSubject> mappings = new java.util.ArrayList<>();
    for (int index = 0; index < 9; index++) {
      UUID subjectId = UUID.randomUUID();
      StandardSubject mapping =
          new StandardSubject(schoolId, standardId, subjectId, (short) (index + 1));
      mapping.setMaximumMarks(index == 7 ? 400 : 200);
      mappings.add(mapping);
      when(subjects.findByIdAndSchoolId(subjectId, schoolId))
          .thenReturn(
              Optional.of(
                  new Subject(schoolId, "S" + index, "Subject " + index, (short) (index + 1))));
    }
    when(standardSubjects.findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId))
        .thenReturn(mappings);

    // totalWorkingDays is ignored / not required for EKAM_KASOTI
    service.processExcelUpload(ekamKasotiWorkbook(), null, ExamResultService.EKAM_KASOTI);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AnnualExamResult>> resultCaptor = ArgumentCaptor.forClass(List.class);
    verify(results).deleteBySchoolIdAndResultType(schoolId, ExamResultService.EKAM_KASOTI);
    verify(results).saveAll(resultCaptor.capture());

    AnnualExamResult result = resultCaptor.getValue().getFirst();
    assertThat(result.getResultType()).isEqualTo(ExamResultService.EKAM_KASOTI);
    assertThat(result.getStandard()).isEqualTo("8");
    assertThat(result.getRollNumber()).isEqualTo(1);
    assertThat(result.getAttendedDays()).isNull();
    assertThat(result.getTotalWorkingDays()).isNull();
    assertThat(result.getOverallGrade()).isNull();
    assertThat(result.getSubjects()).hasSize(9);
  }

  @Test
  void importsAnnualWorkbookWithAttendance() throws IOException {
    UUID schoolId = UUID.randomUUID();
    School school = Mockito.mock(School.class);
    when(school.getId()).thenReturn(schoolId);
    when(schools.findFirstByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(school));
    Standard standard = new Standard(schoolId, "8", "8", (short) 8);
    UUID standardId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(standard, "id", standardId);
    when(standards.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId))
        .thenReturn(List.of(standard));
    List<StandardSubject> mappings = new java.util.ArrayList<>();
    for (int index = 0; index < 9; index++) {
      UUID subjectId = UUID.randomUUID();
      StandardSubject mapping =
          new StandardSubject(schoolId, standardId, subjectId, (short) (index + 1));
      mapping.setMaximumMarks(200);
      mappings.add(mapping);
      when(subjects.findByIdAndSchoolId(subjectId, schoolId))
          .thenReturn(
              Optional.of(
                  new Subject(schoolId, "S" + index, "Subject " + index, (short) (index + 1))));
    }
    when(standardSubjects.findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId))
        .thenReturn(mappings);

    service.processExcelUpload(annualWorkbook(), 250, ExamResultService.ANNUAL);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AnnualExamResult>> resultCaptor = ArgumentCaptor.forClass(List.class);
    verify(results).deleteBySchoolIdAndResultType(schoolId, ExamResultService.ANNUAL);
    verify(results).saveAll(resultCaptor.capture());

    AnnualExamResult result = resultCaptor.getValue().getFirst();
    assertThat(result.getResultType()).isEqualTo(ExamResultService.ANNUAL);
    assertThat(result.getStandard()).isEqualTo("8");
    assertThat(result.getRollNumber()).isEqualTo(1);
    assertThat(result.getTotalWorkingDays()).isEqualTo(250);
    assertThat(result.getAttendedDays()).isEqualTo(240);
    assertThat(result.getSubjects()).hasSize(9);
  }

  /**
   * EKAM_KASOTI workbook: no Sr.No., no Hajar Divas. Columns: GR No.(0), Standard(1), Name(2),
   * Birth Date(3), subjects from col 4 in pairs.
   */
  private MockMultipartFile ekamKasotiWorkbook() throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet();
      var header = sheet.createRow(0);
      String[] headers = {
        "GR No.",
        "Standard",
        "Name",
        "Birth Date",
        "Gujarati",
        "Gujarati Grade",
        "Math",
        "Math Grade",
        "Science",
        "Science Grade",
        "Hindi",
        "Hindi Grade",
        "English",
        "English Grade",
        "SS",
        "SS Grade",
        "Sanskrit",
        "Sanskrit Grade",
        "VV",
        "VV Grade",
        "Paryavaran",
        "Paryavaran Grade"
      };
      for (int index = 0; index < headers.length; index++) {
        header.createCell(index).setCellValue(headers[index]);
      }
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("1001"); // GR No.
      row.createCell(1).setCellValue("8"); // Standard
      row.createCell(2).setCellValue("Student One"); // Name
      row.createCell(3).setCellValue("2012-01-01"); // Birth Date
      for (int index = 4; index < headers.length; index += 2) {
        row.createCell(index).setCellValue(75);
        row.createCell(index + 1).setCellValue("A");
      }
      workbook.write(output);
      return new MockMultipartFile(
          "file",
          "ekam-result.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          output.toByteArray());
    }
  }

  /**
   * ANNUAL workbook: no Sr.No., has Hajar Divas. Columns: GR No.(0), Standard(1), Name(2), Birth
   * Date(3), Hajar Divas(4), subjects from col 5.
   */
  private MockMultipartFile annualWorkbook() throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet();
      var header = sheet.createRow(0);
      String[] headers = {
        "GR No.",
        "Standard",
        "Name",
        "Birth Date",
        "Hajar Divas",
        "Gujarati",
        "Gujarati Grade",
        "Math",
        "Math Grade",
        "Science",
        "Science Grade",
        "Hindi",
        "Hindi Grade",
        "English",
        "English Grade",
        "SS",
        "SS Grade",
        "Sanskrit",
        "Sanskrit Grade",
        "VV",
        "VV Grade",
        "Paryavaran",
        "Paryavaran Grade"
      };
      for (int index = 0; index < headers.length; index++) {
        header.createCell(index).setCellValue(headers[index]);
      }
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("1001"); // GR No.
      row.createCell(1).setCellValue("8"); // Standard
      row.createCell(2).setCellValue("Student One"); // Name
      row.createCell(3).setCellValue("2012-01-01"); // Birth Date
      row.createCell(4).setCellValue(240); // Hajar Divas
      for (int index = 5; index < headers.length; index += 2) {
        row.createCell(index).setCellValue(75);
        row.createCell(index + 1).setCellValue("A");
      }
      workbook.write(output);
      return new MockMultipartFile(
          "file",
          "exam-result.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          output.toByteArray());
    }
  }
}
