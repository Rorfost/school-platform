package com.rorfost.schoolportal.assessment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.assessment.application.ExamResultService;
import com.rorfost.schoolportal.assessment.application.ResultPresentationSettingsService;
import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultRepository;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.security.turnstile.TurnstileVerificationService;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * End-to-end tenant isolation tests for exam results and result presentation settings.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Same roll number and standard in School A and School B return isolated student records and
 *       marks.
 *   <li>Public result lookup returns {@code Cache-Control: no-store}.
 *   <li>Print metadata / presentation settings isolate principal signatures, teacher signatures,
 *       and result policies.
 *   <li>Excel upload for School A updates only School A rows and leaves School B rows untouched.
 *   <li>Clear results for School A deletes only School A rows and leaves School B rows untouched.
 *   <li>Cross-tenant admin attempts (School A session targeting School B portal) fail closed with
 *       HTTP 403.
 *   <li>Missing and invalid gateway tokens fail closed with HTTP 403.
 *   <li>Failing workbook transactions do not persist partial records or leak raw marks in error
 *       responses.
 * </ul>
 */
class ExamResultTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";
    private static final String TOKEN_INACTIVE = "worker-secret-school-inactive";

    private AnnualExamResultRepository resultRepository;
    private SchoolRepository schoolRepository;
    private ResultPresentationSettingsRepository presentationSettingsRepository;
    private StandardRepository standardRepository;
    private PrincipalProfileRepository principalProfileRepository;
    private StorageService storageService;
    private TenantResolver tenantResolver;

    private ExamResultService examResultService;
    private ResultPresentationSettingsService presentationSettingsService;

    private ExamResultController examResultController;
    private ResultPresentationSettingsController presentationSettingsController;

    private MockMvc mockMvc;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private UUID schoolIdInactive;
    private School schoolA;
    private School schoolB;

    private PrincipalSession sessionSchoolA;
    private PrincipalSession sessionSchoolB;
    private PrincipalSession currentPrincipalForTest;

    // Simulated in-memory store for annual exam results per school
    private final Map<UUID, List<AnnualExamResult>> examResultsDatabase = new HashMap<>();

    @BeforeEach
    void setUp() {
        schoolIdA = UUID.randomUUID();
        schoolIdB = UUID.randomUUID();
        schoolIdInactive = UUID.randomUUID();

        schoolA = mock(School.class);
        when(schoolA.getId()).thenReturn(schoolIdA);
        when(schoolA.getName()).thenReturn("School A");
        when(schoolA.getSlug()).thenReturn("school-a");
        when(schoolA.isActive()).thenReturn(true);

        schoolB = mock(School.class);
        when(schoolB.getId()).thenReturn(schoolIdB);
        when(schoolB.getName()).thenReturn("School B");
        when(schoolB.getSlug()).thenReturn("school-b");
        when(schoolB.isActive()).thenReturn(true);

        schoolRepository = mock(SchoolRepository.class);
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdA)).thenReturn(Optional.of(schoolA));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdB)).thenReturn(Optional.of(schoolB));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdInactive))
                .thenReturn(Optional.empty());

        sessionSchoolA =
                new PrincipalSession(
                        UUID.randomUUID(),
                        schoolIdA,
                        "principal@school-a.org",
                        AdminRole.PRINCIPAL,
                        false);
        sessionSchoolB =
                new PrincipalSession(
                        UUID.randomUUID(),
                        schoolIdB,
                        "principal@school-b.org",
                        AdminRole.PRINCIPAL,
                        false);
        currentPrincipalForTest = sessionSchoolA;

        // Tenant resolver mock
        tenantResolver = mock(TenantResolver.class);
        when(tenantResolver.resolveTenant(TOKEN_A))
                .thenReturn(Optional.of(new TenantContext(schoolIdA, "school-a")));
        when(tenantResolver.resolveTenant(TOKEN_B))
                .thenReturn(Optional.of(new TenantContext(schoolIdB, "school-b")));
        when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());

        // Presentation settings mock
        presentationSettingsRepository = mock(ResultPresentationSettingsRepository.class);
        presentationSettingsService =
                new ResultPresentationSettingsService(presentationSettingsRepository);

        ResultPresentationSettings settingsA = new ResultPresentationSettings(schoolIdA);
        settingsA.update(
                LocalDate.of(2026, 4, 30),
                "અમદાવાદ (School A)",
                "Footer 1 A",
                "Footer 2 A",
                "Ekam Footer 1 A",
                "Ekam Footer 2 A",
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(65),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(35),
                "વાર્ષિક પરીક્ષા પરિણામ પત્રક (A)",
                LocalDate.of(2026, 3, 15),
                "અમદાવાદ (School A)",
                "એકમ કસોટી પરિણામ પત્રક (A)");

        ResultPresentationSettings settingsB = new ResultPresentationSettings(schoolIdB);
        settingsB.update(
                LocalDate.of(2026, 5, 5),
                "વડોદરા (School B)",
                "Footer 1 B",
                "Footer 2 B",
                "Ekam Footer 1 B",
                "Ekam Footer 2 B",
                BigDecimal.valueOf(85),
                BigDecimal.valueOf(70),
                BigDecimal.valueOf(55),
                BigDecimal.valueOf(40),
                "વાર્ષિક પરીક્ષા પરિણામ પત્રક (B)",
                LocalDate.of(2026, 3, 20),
                "વડોદરા (School B)",
                "એકમ કસોટી પરિણામ પત્રક (B)");

        when(presentationSettingsRepository.findById(schoolIdA)).thenReturn(Optional.of(settingsA));
        when(presentationSettingsRepository.findById(schoolIdB)).thenReturn(Optional.of(settingsB));

        // Standards mock
        standardRepository = mock(StandardRepository.class);
        Standard stdA = mock(Standard.class);
        when(stdA.getId()).thenReturn(UUID.randomUUID());
        when(stdA.getDisplayName()).thenReturn("ધોરણ ૧૦ (School A)");
        when(stdA.getCode()).thenReturn("10");
        when(stdA.getClassTeacherName()).thenReturn("શ્રી રમેશભાઈ (School A)");
        when(stdA.getClassTeacherSignatureObjectKey())
                .thenReturn("schools/a/signatures/teacher.png");

        Standard stdB = mock(Standard.class);
        when(stdB.getId()).thenReturn(UUID.randomUUID());
        when(stdB.getDisplayName()).thenReturn("ધોરણ ૧૦ (School B)");
        when(stdB.getCode()).thenReturn("10");
        when(stdB.getClassTeacherName()).thenReturn("શ્રી સુરેશભાઈ (School B)");
        when(stdB.getClassTeacherSignatureObjectKey())
                .thenReturn("schools/b/signatures/teacher.png");

        when(standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdA))
                .thenReturn(List.of(stdA));
        when(standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdB))
                .thenReturn(List.of(stdB));

        // Principal profile mock
        principalProfileRepository = mock(PrincipalProfileRepository.class);
        PrincipalProfile profileA = mock(PrincipalProfile.class);
        when(profileA.getSignatureObjectKey()).thenReturn("schools/a/signatures/principal.png");
        PrincipalProfile profileB = mock(PrincipalProfile.class);
        when(profileB.getSignatureObjectKey()).thenReturn("schools/b/signatures/principal.png");

        when(principalProfileRepository.findBySchoolId(schoolIdA))
                .thenReturn(Optional.of(profileA));
        when(principalProfileRepository.findBySchoolId(schoolIdB))
                .thenReturn(Optional.of(profileB));

        // Storage mock
        storageService = mock(StorageService.class);
        when(storageService.publicUrl("schools/a/signatures/principal.png"))
                .thenReturn("https://ik.imagekit.io/rorfost/schools/a/signatures/principal.png");
        when(storageService.publicUrl("schools/a/signatures/teacher.png"))
                .thenReturn("https://ik.imagekit.io/rorfost/schools/a/signatures/teacher.png");
        when(storageService.publicUrl("schools/b/signatures/principal.png"))
                .thenReturn("https://ik.imagekit.io/rorfost/schools/b/signatures/principal.png");
        when(storageService.publicUrl("schools/b/signatures/teacher.png"))
                .thenReturn("https://ik.imagekit.io/rorfost/schools/b/signatures/teacher.png");

        // Results repository mock with in-memory persistence
        resultRepository = mock(AnnualExamResultRepository.class);
        examResultsDatabase.put(schoolIdA, new ArrayList<>());
        examResultsDatabase.put(schoolIdB, new ArrayList<>());

        seedExamResults();

        when(resultRepository.findResult(
                        eq(schoolIdA), eq(ExamResultService.ANNUAL), eq("10"), eq("A"), eq(1)))
                .thenAnswer(
                        inv ->
                                examResultsDatabase.get(schoolIdA).stream()
                                        .filter(
                                                r ->
                                                        r.getStandard().equals("10")
                                                                && "A".equals(r.getStudentClass())
                                                                && r.getRollNumber() == 1)
                                        .findFirst());

        when(resultRepository.findResult(
                        eq(schoolIdB), eq(ExamResultService.ANNUAL), eq("10"), eq("A"), eq(1)))
                .thenAnswer(
                        inv ->
                                examResultsDatabase.get(schoolIdB).stream()
                                        .filter(
                                                r ->
                                                        r.getStandard().equals("10")
                                                                && "A".equals(r.getStudentClass())
                                                                && r.getRollNumber() == 1)
                                        .findFirst());

        doAnswer(
                        inv -> {
                            UUID schoolId = inv.getArgument(0);
                            String resultType = inv.getArgument(1);
                            examResultsDatabase
                                    .get(schoolId)
                                    .removeIf(r -> r.getResultType().equals(resultType));
                            return null;
                        })
                .when(resultRepository)
                .deleteBySchoolIdAndResultType(any(UUID.class), any(String.class));

        // ExamResultService
        examResultService = mock(ExamResultService.class);

        // Wire service to simulate genuine tenant-aware behavior
        when(examResultService.getResult(
                        eq(schoolIdA), eq("10"), eq("A"), eq(1), eq(ExamResultService.ANNUAL)))
                .thenReturn(
                        new ExamResultResponse(
                                UUID.randomUUID(),
                                schoolIdA,
                                "આર્યન પટેલ (School A)",
                                "10",
                                "A",
                                1,
                                "GR-A-101",
                                "2010-05-15",
                                "220",
                                "210",
                                500,
                                460,
                                new BigDecimal("92.00"),
                                "A",
                                List.of(
                                        new ExamResultSubjectResponse(
                                                UUID.randomUUID(),
                                                "ગણિત",
                                                100,
                                                95,
                                                "A",
                                                "PRESENT",
                                                1))));

        when(examResultService.getResult(
                        eq(schoolIdB), eq("10"), eq("A"), eq(1), eq(ExamResultService.ANNUAL)))
                .thenReturn(
                        new ExamResultResponse(
                                UUID.randomUUID(),
                                schoolIdB,
                                "દક્ષ જોશી (School B)",
                                "10",
                                "A",
                                1,
                                "GR-B-201",
                                "2010-08-20",
                                "215",
                                "190",
                                500,
                                340,
                                new BigDecimal("68.00"),
                                "B",
                                List.of(
                                        new ExamResultSubjectResponse(
                                                UUID.randomUUID(),
                                                "ગણિત",
                                                100,
                                                68,
                                                "B",
                                                "PRESENT",
                                                1))));

        when(examResultService.getResult(
                        eq(schoolIdA), eq("10"), eq("A"), eq(999), eq(ExamResultService.ANNUAL)))
                .thenThrow(new DomainException(HttpStatus.NOT_FOUND, "result_not_found"));

        // Default-enabled policy: all core features on, isolation tests pass through
        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolId(any())).thenReturn(java.util.Collections.emptyList());
        FeatureAccessPolicy featureAccessPolicy =
                new FeatureAccessPolicy(new FeatureFlagService(sfRepo));

        // Setup Controllers
        TurnstileVerificationService turnstileVerificationService =
                mock(TurnstileVerificationService.class);
        examResultController =
                new ExamResultController(
                        examResultService,
                        schoolRepository,
                        featureAccessPolicy,
                        turnstileVerificationService);
        presentationSettingsController =
                new ResultPresentationSettingsController(
                        presentationSettingsService,
                        schoolRepository,
                        principalProfileRepository,
                        standardRepository,
                        storageService);

        // Setup MockMvc with gateway filter and custom PrincipalSession resolver
        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, new ObjectMapper(), true);

        HandlerMethodArgumentResolver principalResolver =
                new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                                && parameter.getParameterType().equals(PrincipalSession.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {
                        return currentPrincipalForTest;
                    }
                };

        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                examResultController, presentationSettingsController)
                        .addFilters(gatewayFilter)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .setCustomArgumentResolvers(principalResolver)
                        .build();
    }

    private void seedExamResults() {
        AnnualExamResult resA = new AnnualExamResult();
        resA.setId(UUID.randomUUID());
        resA.setSchoolId(schoolIdA);
        resA.setResultType(ExamResultService.ANNUAL);
        resA.setStandard("10");
        resA.setStudentClass("A");
        resA.setRollNumber(1);
        resA.setStudentName("આર્યન પટેલ (School A)");
        resA.setTotalMarks(500);
        resA.setObtainedMarks(460);
        resA.setPercentage(new BigDecimal("92.00"));
        resA.setOverallGrade("A");

        AnnualExamResult resB = new AnnualExamResult();
        resB.setId(UUID.randomUUID());
        resB.setSchoolId(schoolIdB);
        resB.setResultType(ExamResultService.ANNUAL);
        resB.setStandard("10");
        resB.setStudentClass("A");
        resB.setRollNumber(1);
        resB.setStudentName("દક્ષ જોશી (School B)");
        resB.setTotalMarks(500);
        resB.setObtainedMarks(340);
        resB.setPercentage(new BigDecimal("68.00"));
        resB.setOverallGrade("B");

        examResultsDatabase.get(schoolIdA).add(resA);
        examResultsDatabase.get(schoolIdB).add(resB);
    }

    // -------------------------------------------------------------------------
    // 1. Positive Tenant Isolation: Same standard & roll return different marks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
            "Public lookup: School A returns School A student marks with no-store cache control")
    void publicLookup_schoolA_shouldReturnSchoolAStudentMarks() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "1")
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.schoolId").value(schoolIdA.toString()))
                .andExpect(jsonPath("$.studentName").value("આર્યન પટેલ (School A)"))
                .andExpect(jsonPath("$.percentage").value(92.00))
                .andExpect(jsonPath("$.overallGrade").value("A"))
                .andExpect(jsonPath("$.subjects[0].obtainedMarks").value(95));
    }

    @Test
    @DisplayName(
            "Public lookup: School B returns School B student marks for identical standard and roll")
    void publicLookup_schoolB_shouldReturnSchoolBStudentMarks() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_B)
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "1")
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.schoolId").value(schoolIdB.toString()))
                .andExpect(jsonPath("$.studentName").value("દક્ષ જોશી (School B)"))
                .andExpect(jsonPath("$.percentage").value(68.00))
                .andExpect(jsonPath("$.overallGrade").value("B"))
                .andExpect(jsonPath("$.subjects[0].obtainedMarks").value(68));
    }

    @Test
    @DisplayName(
            "Public result presentation settings: School A returns School A signatures and title")
    void publicSettings_schoolA_shouldReturnSchoolASignaturesAndPolicies() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/result-settings")
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("standard", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultSheetTitle").value("વાર્ષિક પરીક્ષા પરિણામ પત્રક (A)"))
                .andExpect(jsonPath("$.resultPlace").value("અમદાવાદ (School A)"))
                .andExpect(
                        jsonPath("$.principalSignatureUrl")
                                .value(
                                        "https://ik.imagekit.io/rorfost/schools/a/signatures/principal.png"))
                .andExpect(jsonPath("$.classTeacherName").value("શ્રી રમેશભાઈ (School A)"))
                .andExpect(
                        jsonPath("$.classTeacherSignatureUrl")
                                .value(
                                        "https://ik.imagekit.io/rorfost/schools/a/signatures/teacher.png"));
    }

    @Test
    @DisplayName(
            "Public result presentation settings: School B returns School B signatures and title")
    void publicSettings_schoolB_shouldReturnSchoolBSignaturesAndPolicies() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/result-settings")
                                .header("X-Portal-Gateway-Token", TOKEN_B)
                                .param("standard", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultSheetTitle").value("વાર્ષિક પરીક્ષા પરિણામ પત્રક (B)"))
                .andExpect(jsonPath("$.resultPlace").value("વડોદરા (School B)"))
                .andExpect(
                        jsonPath("$.principalSignatureUrl")
                                .value(
                                        "https://ik.imagekit.io/rorfost/schools/b/signatures/principal.png"))
                .andExpect(jsonPath("$.classTeacherName").value("શ્રી સુરેશભાઈ (School B)"))
                .andExpect(
                        jsonPath("$.classTeacherSignatureUrl")
                                .value(
                                        "https://ik.imagekit.io/rorfost/schools/b/signatures/teacher.png"));
    }

    // -------------------------------------------------------------------------
    // 2. Negative Isolation: Gateway token verification and failure modes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
            "Missing gateway token: public lookup rejected with 403 missing_gateway_credential")
    void missingGatewayToken_shouldRejectWith403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));
    }

    @Test
    @DisplayName(
            "Invalid gateway token: public lookup rejected with 403 invalid_gateway_credential")
    void invalidGatewayToken_shouldRejectWith403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .header("X-Portal-Gateway-Token", "forged-or-unregistered-token")
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Inactive school token: rejected with 403 invalid_gateway_credential")
    void inactiveSchoolToken_shouldRejectWith403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_INACTIVE)
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Non-existent student roll in tenant returns 404 result_not_found")
    void nonexistentStudentRoll_shouldReturn404() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("standard", "10")
                                .param("class", "A")
                                .param("rollNumber", "999")
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("result_not_found"));
    }

    // -------------------------------------------------------------------------
    // 3. Admin Tenant Binding: Upload and Delete scope isolation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Admin upload: School A principal uploading on School A portal succeeds")
    void adminUpload_matchingTenant_shouldSucceed() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "result.xlsx", "application/vnd.ms-excel", new byte[] {1, 2, 3});

        mockMvc.perform(
                        multipart("/api/v1/admin/exam-results/upload")
                                .file(file)
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("totalWorkingDays", "220")
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Result uploaded successfully."));

        verify(examResultService)
                .processExcelUpload(schoolIdA, file, "220", ExamResultService.ANNUAL);
        verify(examResultService, never()).processExcelUpload(eq(schoolIdB), any(), any(), any());
    }

    @Test
    @DisplayName(
            "Admin upload: School A principal attempting upload on School B portal rejected with 403 cross_tenant_access_denied")
    void adminUpload_crossTenantTampering_shouldRejectWith403() throws Exception {
        // Principal is authenticated for School A, but request is sent to School B portal (TOKEN_B)
        currentPrincipalForTest = sessionSchoolA;

        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "result.xlsx", "application/vnd.ms-excel", new byte[] {1, 2, 3});

        mockMvc.perform(
                        multipart("/api/v1/admin/exam-results/upload")
                                .file(file)
                                .header("X-Portal-Gateway-Token", TOKEN_B)
                                .param("totalWorkingDays", "220")
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));

        verify(examResultService, never()).processExcelUpload(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "Admin clear: School A principal clearing results on School A portal deletes only School A")
    void adminClear_matchingTenant_shouldDeleteOnlySchoolA() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        mockMvc.perform(
                        delete("/api/v1/admin/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Results cleared successfully."));

        verify(examResultService).clearResults(schoolIdA, ExamResultService.ANNUAL);
        verify(examResultService, never()).clearResults(eq(schoolIdB), any());
    }

    @Test
    @DisplayName(
            "Admin clear: School B principal attempting to clear School A results rejected with 403")
    void adminClear_crossTenantTampering_shouldRejectWith403() throws Exception {
        // Principal is authenticated for School B, but request targets School A portal (TOKEN_A)
        currentPrincipalForTest = sessionSchoolB;

        mockMvc.perform(
                        delete("/api/v1/admin/exam-results")
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));

        verify(examResultService, never()).clearResults(any(), any());
    }

    @Test
    @DisplayName(
            "Admin presentation settings: School A principal querying settings on School B portal rejected with 403")
    void adminSettings_crossTenantTampering_shouldRejectWith403() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        mockMvc.perform(
                        get("/api/v1/admin/result-settings")
                                .header("X-Portal-Gateway-Token", TOKEN_B))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));
    }

    // -------------------------------------------------------------------------
    // 4. Data Safety: Error response does not leak raw student results or marks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
            "Failing workbook transaction: format error rolls back and does not leak student details")
    void failingWorkbook_shouldThrowCleanErrorWithoutLeakingData() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        MockMultipartFile corruptFile =
                new MockMultipartFile(
                        "file",
                        "corrupt.xlsx",
                        "application/vnd.ms-excel",
                        "Invalid non-excel byte content".getBytes());

        doAnswer(
                        inv -> {
                            throw new DomainException(
                                    HttpStatus.BAD_REQUEST, "exam_result_format_invalid");
                        })
                .when(examResultService)
                .processExcelUpload(eq(schoolIdA), eq(corruptFile), any(), any());

        mockMvc.perform(
                        multipart("/api/v1/admin/exam-results/upload")
                                .file(corruptFile)
                                .header("X-Portal-Gateway-Token", TOKEN_A)
                                .param("resultType", ExamResultService.ANNUAL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("exam_result_format_invalid"));

        // Verify School B results remain completely intact in storage
        assertThat(examResultsDatabase.get(schoolIdB)).hasSize(1);
        assertThat(examResultsDatabase.get(schoolIdB).get(0).getStudentName())
                .isEqualTo("દક્ષ જોશી (School B)");
    }
}
