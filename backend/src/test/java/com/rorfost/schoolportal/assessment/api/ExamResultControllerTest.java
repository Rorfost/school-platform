package com.rorfost.schoolportal.assessment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.assessment.application.ExamResultService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class ExamResultControllerTest {

    private ExamResultService service;
    private SchoolRepository schools;
    private ExamResultController controller;
    private FeatureAccessPolicy defaultEnabledPolicy;

    private UUID schoolId;
    private School school;
    private PrincipalSession principal;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        service = mock(ExamResultService.class);
        schools = mock(SchoolRepository.class);
        // Build a default-enabled policy (no overrides → all core features on)
        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolId(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Collections.emptyList());
        defaultEnabledPolicy = new FeatureAccessPolicy(new FeatureFlagService(sfRepo));
        controller = new ExamResultController(service, schools, defaultEnabledPolicy);

        schoolId = UUID.randomUUID();
        school = mock(School.class);
        when(school.getId()).thenReturn(schoolId);
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        principal =
                new PrincipalSession(
                        UUID.randomUUID(),
                        schoolId,
                        "admin@school.org",
                        AdminRole.PRINCIPAL,
                        false);

        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TenantContextHolder.setTenantContext(request, new TenantContext(schoolId, "school-slug"));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getResult_shouldReturnResultWithNoStoreCacheControl() {
        UUID resultId = UUID.randomUUID();

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

        when(service.getResult(schoolId, "5", "A", 1, ExamResultService.ANNUAL))
                .thenReturn(mockResponse);

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
    void getResult_shouldThrowWhenTenantContextMissing() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> controller.getResult("5", "A", 1, ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "school_not_found")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
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
                controller.uploadResults(principal, file, "180", ExamResultService.ANNUAL);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Result uploaded successfully.");
        verify(service).processExcelUpload(schoolId, file, "180", ExamResultService.ANNUAL);
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
                controller.uploadResults(principal, file, null, ExamResultService.EKAM_KASOTI);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Result uploaded successfully.");
        verify(service).processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);
    }

    @Test
    void uploadResults_shouldRejectCrossTenantAttempt() {
        UUID otherSchoolId = UUID.randomUUID();
        PrincipalSession crossTenantPrincipal =
                new PrincipalSession(
                        UUID.randomUUID(),
                        otherSchoolId,
                        "attacker@other.org",
                        AdminRole.PRINCIPAL,
                        false);

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "result.xlsx", "application/vnd.ms-excel", new byte[] {1});

        assertThatThrownBy(
                        () ->
                                controller.uploadResults(
                                        crossTenantPrincipal, file, null, ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "cross_tenant_access_denied")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void uploadResults_shouldRejectMissingTenantContext() {
        RequestContextHolder.resetRequestAttributes();

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "result.xlsx", "application/vnd.ms-excel", new byte[] {1});

        assertThatThrownBy(
                        () ->
                                controller.uploadResults(
                                        principal, file, null, ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "missing_tenant_context")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void clearResults_shouldDelegateToService() {
        ResponseEntity<Map<String, String>> response =
                controller.clearResults(principal, ExamResultService.ANNUAL);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Results cleared successfully.");
        verify(service).clearResults(schoolId, ExamResultService.ANNUAL);
    }

    @Test
    void clearResults_shouldRejectCrossTenantAttempt() {
        UUID otherSchoolId = UUID.randomUUID();
        PrincipalSession crossTenantPrincipal =
                new PrincipalSession(
                        UUID.randomUUID(),
                        otherSchoolId,
                        "attacker@other.org",
                        AdminRole.PRINCIPAL,
                        false);

        assertThatThrownBy(
                        () ->
                                controller.clearResults(
                                        crossTenantPrincipal, ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "cross_tenant_access_denied")
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void publicExamResult_disabledFeature_shouldReturn404() {
        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolIdAndIdFeatureKey(schoolId, FeatureKey.EXAM_RESULTS.name()))
                .thenReturn(
                        Optional.of(new SchoolFeature(schoolId, FeatureKey.EXAM_RESULTS, false)));
        ExamResultController disabledController =
                new ExamResultController(
                        service, schools, new FeatureAccessPolicy(new FeatureFlagService(sfRepo)));
        TenantContextHolder.setTenantContext(request, new TenantContext(schoolId, "test-school"));

        assertThatThrownBy(
                        () -> disabledController.getResult("10", "A", 1, ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void adminUpload_disabledFeature_shouldReturn403() {
        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolIdAndIdFeatureKey(schoolId, FeatureKey.EXAM_RESULTS.name()))
                .thenReturn(
                        Optional.of(new SchoolFeature(schoolId, FeatureKey.EXAM_RESULTS, false)));
        ExamResultController disabledController =
                new ExamResultController(
                        service, schools, new FeatureAccessPolicy(new FeatureFlagService(sfRepo)));
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "results.xlsx", "application/vnd.ms-excel", new byte[] {1, 2});

        assertThatThrownBy(
                        () ->
                                disabledController.uploadResults(
                                        principal, file, "180", ExamResultService.ANNUAL))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }
}
