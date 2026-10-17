package com.rorfost.schoolportal.school.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.school.application.VisitService;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tenant isolation test suite for public visit counters and admin metrics reset.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>3 visits to School A and 1 visit to School B result in strictly independent counts.
 *   <li>Unauthenticated/unregistered gateway credentials cannot increment or view visit metrics.
 *   <li>Inactive school credentials fail closed with HTTP 403 Forbidden.
 *   <li>Cross-tenant admin attempts to reset another school's visits fail with HTTP 403 Forbidden.
 *   <li>Admin reset for School A zeroes only School A's counter, leaving School B untouched.
 * </ul>
 */
class VisitTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";
    private static final String TOKEN_INACTIVE = "worker-secret-school-inactive";

    private UUID schoolIdA;
    private UUID schoolIdB;
    private UUID schoolIdInactive;
    private School schoolA;
    private School schoolB;

    private SchoolRepository schoolRepository;
    private TenantResolver tenantResolver;
    private JdbcTemplate jdbcTemplate;
    private VisitService visitService;
    private VisitController visitController;
    private AdminVisitController adminVisitController;
    private MockMvc mockMvc;

    private PrincipalSession sessionSchoolA;
    private PrincipalSession sessionSchoolB;
    private PrincipalSession currentPrincipalForTest;

    private final Map<UUID, Long> visitsDatabase = new ConcurrentHashMap<>();

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

        tenantResolver = mock(TenantResolver.class);
        when(tenantResolver.resolveTenant(TOKEN_A))
                .thenReturn(Optional.of(new TenantContext(schoolIdA, "school-a")));
        when(tenantResolver.resolveTenant(TOKEN_B))
                .thenReturn(Optional.of(new TenantContext(schoolIdB, "school-b")));
        when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());

        // Simulated in-memory site_metrics table via JdbcTemplate
        visitsDatabase.clear();
        visitsDatabase.put(schoolIdA, 0L);
        visitsDatabase.put(schoolIdB, 0L);

        jdbcTemplate = mock(JdbcTemplate.class);

        doAnswer(
                        inv -> {
                            UUID id = inv.getArgument(1);
                            String sql = inv.getArgument(0);
                            if (sql.contains("SET total_visits = 0")) {
                                visitsDatabase.put(id, 0L);
                            } else {
                                visitsDatabase.compute(id, (k, v) -> (v == null ? 0L : v) + 1L);
                            }
                            return 1;
                        })
                .when(jdbcTemplate)
                .update(anyString(), any(UUID.class));

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(UUID.class)))
                .thenAnswer(
                        inv -> {
                            UUID id = inv.getArgument(2);
                            return visitsDatabase.getOrDefault(id, 0L);
                        });

        visitService = new VisitService(jdbcTemplate, schoolRepository);
        visitController = new VisitController(visitService, schoolRepository);
        adminVisitController = new AdminVisitController(visitService, schoolRepository);

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
                MockMvcBuilders.standaloneSetup(visitController, adminVisitController)
                        .addFilters(gatewayFilter)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .setCustomArgumentResolvers(principalResolver)
                        .build();
    }

    // -------------------------------------------------------------------------
    // 1. Positive Tenant Isolation: 3 School A visits and 1 School B visit
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
            "Independent visit counts: 3 visits on School A and 1 visit on School B remain strictly isolated")
    void visitCounters_shouldTrackIndependentlyAcrossSchools() throws Exception {
        // Initial state: both schools at 0
        mockMvc.perform(get("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(0));

        mockMvc.perform(get("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(0));

        // School A receives 3 visits
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalVisits").value(i));
        }

        // School B receives 1 visit
        mockMvc.perform(post("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(1));

        // Verify final read counts: School A = 3, School B = 1
        mockMvc.perform(get("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(3));

        mockMvc.perform(get("/api/v1/public/visits").header("X-Portal-Gateway-Token", TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(1));

        assertThat(visitsDatabase.get(schoolIdA)).isEqualTo(3L);
        assertThat(visitsDatabase.get(schoolIdB)).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // 2. Negative Isolation: Gateway token verification and failure modes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
            "Missing gateway token: cannot view or increment visits, fails with 403 missing_gateway_credential")
    void missingGatewayToken_shouldReject() throws Exception {
        mockMvc.perform(get("/api/v1/public/visits"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        mockMvc.perform(post("/api/v1/public/visits"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        // No visits were recorded
        assertThat(visitsDatabase.get(schoolIdA)).isEqualTo(0L);
        assertThat(visitsDatabase.get(schoolIdB)).isEqualTo(0L);
    }

    @Test
    @DisplayName(
            "Invalid gateway token: cannot view or increment visits, fails with 403 invalid_gateway_credential")
    void invalidGatewayToken_shouldReject() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/visits")
                                .header("X-Portal-Gateway-Token", "forged-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));

        mockMvc.perform(
                        post("/api/v1/public/visits")
                                .header("X-Portal-Gateway-Token", "forged-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));

        assertThat(visitsDatabase.get(schoolIdA)).isEqualTo(0L);
        assertThat(visitsDatabase.get(schoolIdB)).isEqualTo(0L);
    }

    @Test
    @DisplayName("Inactive school token: rejected with 403 invalid_gateway_credential")
    void inactiveSchoolToken_shouldReject() throws Exception {
        mockMvc.perform(
                        post("/api/v1/public/visits")
                                .header("X-Portal-Gateway-Token", TOKEN_INACTIVE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    // -------------------------------------------------------------------------
    // 3. Admin Reset Scope & Cross-Tenant Protection
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Admin reset: School A principal resetting on School A zeroes only School A")
    void adminReset_shouldZeroOnlyTargetSchool() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        // Seed counts: A = 5, B = 8
        visitsDatabase.put(schoolIdA, 5L);
        visitsDatabase.put(schoolIdB, 8L);

        mockMvc.perform(
                        post("/api/v1/admin/site-metrics/visits/reset")
                                .header("X-Portal-Gateway-Token", TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits").value(0));

        // School A is reset to 0, School B remains 8
        assertThat(visitsDatabase.get(schoolIdA)).isEqualTo(0L);
        assertThat(visitsDatabase.get(schoolIdB)).isEqualTo(8L);
    }

    @Test
    @DisplayName(
            "Admin reset tampering: School A principal attempting reset on School B portal rejected with 403 cross_tenant_access_denied")
    void adminReset_crossTenantTampering_shouldRejectWith403() throws Exception {
        currentPrincipalForTest = sessionSchoolA;

        visitsDatabase.put(schoolIdA, 5L);
        visitsDatabase.put(schoolIdB, 8L);

        mockMvc.perform(
                        post("/api/v1/admin/site-metrics/visits/reset")
                                .header("X-Portal-Gateway-Token", TOKEN_B))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));

        // Neither school's count was modified
        assertThat(visitsDatabase.get(schoolIdA)).isEqualTo(5L);
        assertThat(visitsDatabase.get(schoolIdB)).isEqualTo(8L);
    }
}
