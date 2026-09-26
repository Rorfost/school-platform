package com.rorfost.schoolportal.assessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  private final ExamResultService service = new ExamResultService(results, schools);

  @Test
  void importsTheSharedWorkbookIntoTheSelectedEkamKasotiResultSet() throws IOException {
    UUID schoolId = UUID.randomUUID();
    School school = Mockito.mock(School.class);
    when(school.getId()).thenReturn(schoolId);
    when(schools.findFirstByIsActiveTrueOrderByCreatedAtAsc()).thenReturn(Optional.of(school));

    service.processExcelUpload(sharedWorkbook(), 250, ExamResultService.EKAM_KASOTI);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<AnnualExamResult>> resultCaptor = ArgumentCaptor.forClass(List.class);
    verify(results).deleteBySchoolIdAndResultType(schoolId, ExamResultService.EKAM_KASOTI);
    verify(results).saveAll(resultCaptor.capture());

    AnnualExamResult result = resultCaptor.getValue().getFirst();
    assertThat(result.getResultType()).isEqualTo(ExamResultService.EKAM_KASOTI);
    assertThat(result.getStandard()).isEqualTo("8");
    assertThat(result.getRollNumber()).isEqualTo(1);
    assertThat(result.getTotalWorkingDays()).isEqualTo(250);
    assertThat(result.getSubjects()).hasSize(9);
  }

  private MockMultipartFile sharedWorkbook() throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet();
      var header = sheet.createRow(0);
      String[] headers = {
        "Sr. No.",
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
      row.createCell(1).setCellValue("1001");
      row.createCell(2).setCellValue("8");
      row.createCell(3).setCellValue("Student One");
      row.createCell(4).setCellValue("2012-01-01");
      row.createCell(5).setCellValue(240);
      for (int index = 6; index < headers.length; index += 2) {
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
