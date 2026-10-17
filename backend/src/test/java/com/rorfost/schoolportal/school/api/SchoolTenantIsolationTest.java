package com.rorfost.schoolportal.school.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.school.application.SchoolService;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

class SchoolTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";
    private static final String TOKEN_INACTIVE = "worker-secret-school-inactive";

    private SchoolRepository schoolRepository;
    private PrincipalProfileRepository principalProfileRepository;
    private StorageService storageService;
    private TenantResolver tenantResolver;
    private SchoolController schoolController;
    private MockMvc mockMvc;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private UUID schoolIdInactive;
    private School schoolA;
    private School schoolB;
    private School schoolInactive;
    private PrincipalProfile profileA;

    @BeforeEach
    void setUp() {
        schoolRepository = Mockito.mock(SchoolRepository.class);
        principalProfileRepository = Mockito.mock(PrincipalProfileRepository.class);
        storageService = Mockito.mock(StorageService.class);
        AuditLogService auditLogService = Mockito.mock(AuditLogService.class);
        tenantResolver = Mockito.mock(TenantResolver.class);
        ObjectMapper objectMapper = new ObjectMapper();

        SchoolService schoolService =
                new SchoolService(
                        schoolRepository,
                        principalProfileRepository,
                        auditLogService,
                        storageService);

        schoolController = new SchoolController(schoolService);

        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        mockMvc =
                MockMvcBuilders.standaloneSetup(schoolController)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();

        // Synthetic School A with logo and contact
        schoolIdA = UUID.randomUUID();
        schoolA = new School("School A Primary", "school-a");
        schoolA.update(
                "School A Primary",
                "School A",
                "24010100001",
                "Address A, Village A",
                "Patan",
                "Sami",
                "Patan",
                "Gujarat",
                "384245",
                "contact@school-a.test",
                "9876543211",
                "https://school-a.example",
                null,
                "About School A",
                "1990",
                "Gujarati",
                "Primary");
        schoolA.changeLogo("branding/school-a/logo.png");

        // Synthetic School B with different logo and contact
        schoolIdB = UUID.randomUUID();
        schoolB = new School("School B Academy", "school-b");
        schoolB.update(
                "School B Academy",
                "School B",
                "24010100002",
                "Address B, Village B",
                "Radhanpur",
                "Radhanpur",
                "Patan",
                "Gujarat",
                "385340",
                "contact@school-b.test",
                "9876543222",
                "https://school-b.example",
                null,
                "About School B",
                "2005",
                "Gujarati",
                "Primary");
        schoolB.changeLogo("branding/school-b/logo.png");

        // Synthetic Inactive School
        schoolIdInactive = UUID.randomUUID();
        schoolInactive = new School("School Inactive", "school-inactive");

        // Principal Profile for School A only
        profileA = new PrincipalProfile(schoolIdA, "Principal A Name");
        profileA.update(
                "Principal A Name",
                "Bio A",
                "M.A., B.Ed.",
                "Headmaster A",
                "Welcome message A",
                "portraits/school-a/principal.jpg",
                "principal@school-a.test",
                "9876543299",
                true,
                true);

        // Gateway token resolutions
        TenantContext contextA = new TenantContext(schoolIdA, "school-a");
        TenantContext contextB = new TenantContext(schoolIdB, "school-b");
        Mockito.when(tenantResolver.resolveTenant(TOKEN_A)).thenReturn(Optional.of(contextA));
        Mockito.when(tenantResolver.resolveTenant(TOKEN_B)).thenReturn(Optional.of(contextB));
        Mockito.when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());

        // Repository lookups
        Mockito.when(schoolRepository.findByIdAndIsActiveTrue(schoolIdA))
                .thenReturn(Optional.of(schoolA));
        Mockito.when(schoolRepository.findByIdAndIsActiveTrue(schoolIdB))
                .thenReturn(Optional.of(schoolB));
        Mockito.when(schoolRepository.findByIdAndIsActiveTrue(schoolIdInactive))
                .thenReturn(Optional.empty());

        // Principal profile lookups (School A has profile, School B has NO profile)
        Mockito.when(principalProfileRepository.findBySchoolId(schoolIdA))
                .thenReturn(Optional.of(profileA));
        Mockito.when(principalProfileRepository.findBySchoolId(schoolIdB))
                .thenReturn(Optional.empty());

        // Storage URLs
        Mockito.when(storageService.publicUrl("branding/school-a/logo.png"))
                .thenReturn("https://ik.imagekit.io/test/branding/school-a/logo.png");
        Mockito.when(storageService.publicUrl("branding/school-b/logo.png"))
                .thenReturn("https://ik.imagekit.io/test/branding/school-b/logo.png");
    }

    @Test
    @DisplayName("GET /api/v1/public/school with Token A returns School A metadata")
    void publicSchool_withTokenA_returnsSchoolA() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/school")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School A Primary"))
                .andExpect(jsonPath("$.schoolCode").value("24010100001"))
                .andExpect(jsonPath("$.email").value("contact@school-a.test"))
                .andExpect(jsonPath("$.phone").value("9876543211"))
                .andExpect(
                        jsonPath("$.logoUrl")
                                .value("https://ik.imagekit.io/test/branding/school-a/logo.png"));
    }

    @Test
    @DisplayName("GET /api/v1/public/school with Token B returns School B metadata")
    void publicSchool_withTokenB_returnsSchoolB() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/school")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School B Academy"))
                .andExpect(jsonPath("$.schoolCode").value("24010100002"))
                .andExpect(jsonPath("$.email").value("contact@school-b.test"))
                .andExpect(jsonPath("$.phone").value("9876543222"))
                .andExpect(
                        jsonPath("$.logoUrl")
                                .value("https://ik.imagekit.io/test/branding/school-b/logo.png"));
    }

    @Test
    @DisplayName("GET /api/v1/public/school ignores malicious spoofed slug and query params")
    void publicSchool_withSpoofedSlug_derivesStrictlyFromVerifiedToken() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/school")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B)
                                .header("X-Portal-School-Slug", "school-a")
                                .param("schoolId", schoolIdA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School B Academy"))
                .andExpect(jsonPath("$.schoolCode").value("24010100002"))
                .andExpect(jsonPath("$.phone").value("9876543222"));
    }

    @Test
    @DisplayName("GET /api/v1/public/principal-profile with Token A returns School A profile")
    void publicProfile_withTokenA_returnsProfileA() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/principal-profile")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Principal A Name"))
                .andExpect(jsonPath("$.designation").value("Headmaster A"))
                .andExpect(jsonPath("$.email").value("principal@school-a.test"))
                .andExpect(jsonPath("$.phone").value("9876543299"));
    }

    @Test
    @DisplayName(
            "GET /api/v1/public/principal-profile with Token B when profile missing returns 404 and does NOT fallback to School A")
    void publicProfile_withTokenB_missingProfile_doesNotFallbackToSchoolA() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/principal-profile")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("principal_profile_not_found"));
    }

    @Test
    @DisplayName(
            "GET /api/v1/public/principal-profile with Token B when profile is not public returns 404")
    void publicProfile_withTokenB_nonPublicProfile_returnsNotFound() throws Exception {
        PrincipalProfile privateProfile = new PrincipalProfile(schoolIdB, "Private Principal");
        privateProfile.update(
                "Private Principal", null, null, "Principal", null, null, null, null, false, false);
        Mockito.when(principalProfileRepository.findBySchoolId(schoolIdB))
                .thenReturn(Optional.of(privateProfile));

        mockMvc.perform(
                        get("/api/v1/public/principal-profile")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("principal_profile_not_found"));
    }

    @Test
    @DisplayName("Public endpoints reject missing gateway token with 403 Forbidden")
    void publicEndpoints_rejectMissingGatewayTokenWith403() throws Exception {
        mockMvc.perform(get("/api/v1/public/school"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        mockMvc.perform(get("/api/v1/public/principal-profile"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));
    }

    @Test
    @DisplayName("Public endpoints reject invalid or inactive gateway token with 403 Forbidden")
    void publicEndpoints_rejectInvalidOrInactiveGatewayTokenWith403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/school")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "unknown-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));

        mockMvc.perform(
                        get("/api/v1/public/school")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_INACTIVE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Principal admin profile remains protected and isolated per authenticated session")
    void adminPrincipalProfile_remainsProtectedAndIsolated() {
        UUID adminUserIdA = UUID.randomUUID();
        PrincipalSession principalSessionA =
                new PrincipalSession(
                        adminUserIdA,
                        schoolIdA,
                        "principal@school-a.test",
                        AdminRole.PRINCIPAL,
                        false);

        PrincipalProfileResponse adminResponse = schoolController.profile(principalSessionA);

        assertThat(adminResponse).isNotNull();
        assertThat(adminResponse.fullName()).isEqualTo("Principal A Name");
        assertThat(adminResponse.email()).isEqualTo("principal@school-a.test");

        // Verify School B admin session queries School B only
        UUID adminUserIdB = UUID.randomUUID();
        PrincipalSession principalSessionB =
                new PrincipalSession(
                        adminUserIdB,
                        schoolIdB,
                        "principal@school-b.test",
                        AdminRole.PRINCIPAL,
                        false);

        PrincipalProfileResponse adminResponseB = schoolController.profile(principalSessionB);
        assertThat(adminResponseB).isNotNull();
        // Since School B has no profile row yet, admin response returns safe empty defaults
        assertThat(adminResponseB.fullName()).isEmpty();
    }
}
