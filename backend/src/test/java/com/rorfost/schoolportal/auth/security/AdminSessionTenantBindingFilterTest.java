package com.rorfost.schoolportal.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.security.SensitiveResponseHeadersFilter;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Tenant isolation and authorization test suite for admin session binding and cookie/header
 * behavior.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Matching session schoolId and request tenantContext schoolId succeeds with 200 OK.
 *   <li>Cross-tenant session access (/api/v1/admin/** with School A session on School B portal) is
 *       denied with 403 Forbidden ("cross_tenant_access_denied").
 *   <li>Missing tenant context on admin request is denied with 403 Forbidden
 *       ("missing_tenant_context").
 *   <li>Mutations (POST/PUT/DELETE) re-check principal account active status and reject deactivated
 *       accounts with 403 Forbidden ("account_inactive_or_revoked").
 *   <li>Admin responses enforce "Cache-Control: no-store" and "Pragma: no-cache".
 * </ul>
 */
class AdminSessionTenantBindingFilterTest {

    private AdminUserRepository adminUserRepository;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private UUID adminUserIdA;
    private PrincipalSession sessionSchoolA;
    private PrincipalSession sessionSchoolB;
    private AdminUser adminUserA;

    @BeforeEach
    void setUp() {
        adminUserRepository = mock(AdminUserRepository.class);
        objectMapper = new ObjectMapper();

        schoolIdA = UUID.randomUUID();
        schoolIdB = UUID.randomUUID();
        adminUserIdA = UUID.randomUUID();

        sessionSchoolA =
                new PrincipalSession(
                        adminUserIdA,
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

        adminUserA = new AdminUser(schoolIdA, "principal@school-a.org", "hashedPassword");

        AdminSessionTenantBindingFilter bindingFilter =
                new AdminSessionTenantBindingFilter(adminUserRepository, objectMapper);
        SensitiveResponseHeadersFilter headersFilter = new SensitiveResponseHeadersFilter();

        mockMvc =
                MockMvcBuilders.standaloneSetup(new TestAdminController())
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(headersFilter, bindingFilter)
                        .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor authenticatedAs(PrincipalSession principal) {
        return request -> {
            Authentication auth =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, principal.authorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    private RequestPostProcessor withTenant(UUID schoolId, String slug) {
        return request -> {
            TenantContextHolder.setTenantContext(request, new TenantContext(schoolId, slug));
            return request;
        };
    }

    @Test
    @DisplayName("Admin GET on matching tenant succeeds and sets Cache-Control: no-store")
    void adminGet_matchingTenant_succeeds() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdA, "school-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(
                        result ->
                                assertThat(result.getResponse().getHeader("Cache-Control"))
                                        .isEqualTo("no-store"));
    }

    @Test
    @DisplayName(
            "Admin GET with School A session on School B portal is denied with 403 cross_tenant_access_denied")
    void adminGet_crossTenantSession_deniedWith403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdB, "school-b")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));
    }

    @Test
    @DisplayName(
            "Admin request with authenticated session but missing tenant context fails closed with 403")
    void adminGet_missingTenantContext_deniedWith403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-resource").with(authenticatedAs(sessionSchoolA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_tenant_context"));
    }

    @Test
    @DisplayName(
            "Admin POST mutation on matching tenant verifies active account in DB and succeeds")
    void adminPost_matchingTenantActiveUser_succeeds() throws Exception {
        when(adminUserRepository.findById(adminUserIdA)).thenReturn(Optional.of(adminUserA));

        mockMvc.perform(
                        post("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdA, "school-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("created"));

        verify(adminUserRepository).findById(adminUserIdA);
    }

    @Test
    @DisplayName(
            "Admin mutation when principal account is deactivated in DB is rejected with 403 account_inactive_or_revoked")
    void adminPost_deactivatedUser_deniedWith403() throws Exception {
        AdminUser inactiveUser = mock(AdminUser.class);
        when(inactiveUser.isActive()).thenReturn(false);
        when(adminUserRepository.findById(adminUserIdA)).thenReturn(Optional.of(inactiveUser));

        mockMvc.perform(
                        post("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdA, "school-a")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inactive_or_revoked"));
    }

    @Test
    @DisplayName("Admin PUT mutation across tenants is blocked before DB account lookup")
    void adminPut_crossTenantSession_deniedWith403() throws Exception {
        mockMvc.perform(
                        put("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdB, "school-b")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));

        verify(adminUserRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Admin DELETE mutation on matching tenant verifies active account and succeeds")
    void adminDelete_matchingTenantActiveUser_succeeds() throws Exception {
        when(adminUserRepository.findById(adminUserIdA)).thenReturn(Optional.of(adminUserA));

        mockMvc.perform(
                        delete("/api/v1/admin/test-resource")
                                .with(authenticatedAs(sessionSchoolA))
                                .with(withTenant(schoolIdA, "school-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("deleted"));
    }

    @Test
    @DisplayName("Unauthenticated request is passed down filter chain without binding rejection")
    void unauthenticatedRequest_passesThroughFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/test-resource").with(withTenant(schoolIdA, "school-a")))
                .andExpect(status().isOk());
    }

    @RestController
    @RequestMapping("/api/v1/admin/test-resource")
    private static class TestAdminController {

        @GetMapping
        ResponseEntity<Map<String, String>> getResource(
                @AuthenticationPrincipal PrincipalSession principal) {
            return ResponseEntity.ok(Map.of("status", "success"));
        }

        @PostMapping
        ResponseEntity<Map<String, String>> createResource(
                @AuthenticationPrincipal PrincipalSession principal) {
            return ResponseEntity.ok(Map.of("status", "created"));
        }

        @PutMapping
        ResponseEntity<Map<String, String>> updateResource(
                @AuthenticationPrincipal PrincipalSession principal) {
            return ResponseEntity.ok(Map.of("status", "updated"));
        }

        @DeleteMapping
        ResponseEntity<Map<String, String>> deleteResource(
                @AuthenticationPrincipal PrincipalSession principal) {
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
    }
}
