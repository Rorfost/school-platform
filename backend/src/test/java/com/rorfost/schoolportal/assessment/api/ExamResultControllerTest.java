package com.rorfost.schoolportal.assessment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.assessment.application.ExamResultService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ExamResultControllerTest {

    private ExamResultService service;
    private ExamResultController controller;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(ExamResultService.class);
        controller = new ExamResultController(service);
    }

    @Test
    void getResult_shouldReturnResultWithNoStoreCacheControl() {
        UUID resultId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();

        ExamResultSubjectResponse sub1 =
                new ExamResultSubjectResponse(
                        UUID.randomUUID(), "ગુજરાતી", 100, 85, "A", "PRESENT", 1);
        ExamResultSubjectResponse sub2 =
                new ExamResultSubjectResponse(UUID.randomUUID(), "ગણિત", 100, 0, "E", "ABSENT", 2);

        ExamResultResponse mockResponse =
                new ExamResultResponse(
                        resultId,
                        schoolId,
                        "UID-001",
                        "વિશાલ કે. પટેલ",
                        "5",
                        "A",
                        1,
                        "GR-101",
                        "2015-05-10",
                        "180",
                        "165",
                        200,
                        85,
                        new BigDecimal("42.50"),
                        "D",
                        List.of(sub1, sub2));

        when(service.getResult("5", "A", 1, ExamResultService.ANNUAL)).thenReturn(mockResponse);

        ResponseEntity<ExamResultResponse> response =
                controller.getResult("5", "A", 1, ExamResultService.ANNUAL);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().studentName()).isEqualTo("વિશાલ કે. પટેલ");
        assertThat(response.getBody().subjects()).hasSize(2);
        assertThat(response.getBody().subjects().get(1).status()).isEqualTo("ABSENT");
    }

    @Test
    void uploadResults_shouldDelegateAnnualUploadToService() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "annual-result.xlsx",
                        "application/vnd.ms-excel",
                        new byte[] {1, 2, 3});

        ResponseEntity<Map<String, String>> response =
                controller.uploadResults(file, "180", ExamResultService.ANNUAL);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Result uploaded successfully.");
        verify(service).processExcelUpload(file, "180", ExamResultService.ANNUAL);
    }

    @Test
    void uploadResults_shouldDelegateTrimasikUploadToService() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "trimasik-result.xlsx",
                        "application/vnd.ms-excel",
                        new byte[] {4, 5, 6});

        ResponseEntity<Map<String, String>> response =
                controller.uploadResults(file, null, ExamResultService.EKAM_KASOTI);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Result uploaded successfully.");
        verify(service).processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);
    }

    @Test
    void clearResults_shouldDelegateToService() {
        ResponseEntity<Map<String, String>> response =
                controller.clearResults(ExamResultService.ANNUAL);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Results cleared successfully.");
        verify(service).clearResults(ExamResultService.ANNUAL);
    }
}
