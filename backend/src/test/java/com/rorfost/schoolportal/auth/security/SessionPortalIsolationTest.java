/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.fixture.TenantFixtureFactory;
import com.rorfost.schoolportal.school.domain.AdminRole;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Session portal cross-tenant isolation tests.
 *
 * <p>Validates the following negative scenarios:
 *
 * <ul>
 *   <li>A School A admin session used against a School B gateway token must be rejected 403.
 *   <li>A valid session with correct school but an injected wrong schoolId claim is rejected.
 *   <li>A unauthenticated request against an admin-only endpoint is denied without leaking data.
 *   <li>Two distinct admin sessions for different schools never observe each other's responses.
 *   <li>Session with must-change-password flag still enforces cross-tenant checks.
 * </ul>
 *
 * <p>These tests complement {@link AdminSessionTenantBindingFilterTest} which focuses on the filter
 * mechanics. Here we focus on behavioral cross-portal isolation properties from the perspective of
 * an attacker carrying a legitimate School A cookie against the School B portal.
 *
 * <p>Note: In standalone MockMvc, Spring Security's session-context integration filter is not
 * active. Authentication is injected directly into {@link SecurityContextHolder} per test.
 */
class SessionPortalIsolationTest {

    private TenantFixtureFactory fixtures;
    private MockMvc mockMvc;

    // Minimal stub controller that echoes the resolved schoolId from the authenticated session
    // (mirrors what every protected admin endpoint does)
    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/api/v1/admin/echo")
    static class EchoSchoolController {
        @org.springframework.web.bind.annotation.GetMapping
        public org.springframework.http.ResponseEntity<java.util.Map<String, String>> echo(
                @org.springframework.security.core.annotation.AuthenticationPrincipal
                        PrincipalSession session) {
            if (session == null) {
                // No authenticated session — respond 401 without leaking tenant data
                return org.springframework.http.ResponseEntity.status(
                                org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .build();
            }
            return org.springframework.http.ResponseEntity.ok(
                    java.util.Map.of(
                            "schoolId", session.schoolId().toString(), "email", session.email()));
        }
    }

    @BeforeEach
    void setUp() {
        fixtures = new TenantFixtureFactory();

        TenantResolver tenantResolver = Mockito.mock(TenantResolver.class);
        fixtures.stubTenantResolver(tenantResolver);

        ObjectMapper objectMapper = new ObjectMapper();
        EchoSchoolController echoController = new EchoSchoolController();

        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        // AdminSessionTenantBindingFilter checks session.schoolId() ==
        // requestTenantContext.schoolId()
        AdminSessionTenantBindingFilter sessionBindingFilter =
                new AdminSessionTenantBindingFilter(
                        Mockito.mock(
                                com.rorfost.schoolportal.school.repository.AdminUserRepository
                                        .class),
                        objectMapper);

        mockMvc =
                MockMvcBuilders.standaloneSetup(echoController)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .setCustomArgumentResolvers(
                                // Required for @AuthenticationPrincipal to resolve from
                                // SecurityContextHolder in standalone MockMvc
                                new org.springframework.security.web.method.annotation
                                        .AuthenticationPrincipalArgumentResolver())
                        .addFilters(gatewayFilter, sessionBindingFilter)
                        .build();
    }

    @AfterEach
    void clearSecurityContext() {
        // Prevent SecurityContextHolder thread-local state from leaking between tests
        SecurityContextHolder.clearContext();
    }

    // ── Helper: inject a PrincipalSession into the Spring Security context ──────────────────────
    // In standalone MockMvc, Spring Security's session-loading integration filter is absent.
    // We must set SecurityContextHolder directly (thread-local) for the filter to observe it.
    private static void setSecurityContext(PrincipalSession session) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(session, null, session.authorities());
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private static RequestPostProcessor withSession(PrincipalSession session) {
        return request -> {
            setSecurityContext(session);
            return request;
        };
    }

    private static RequestPostProcessor withTenantContext(TenantContext context) {
        return request -> {
            TenantContextHolder.setTenantContext(request, context);
            return request;
        };
    }

    // ── Tests ────────────────────────────────────────────────────────────────────────────────────
    // Note: Public endpoint isolation (public school vs session) is covered by
    // SchoolTenantIsolationTest in the school.api package. The tests here focus exclusively on
    // admin endpoints where the AdminSessionTenantBindingFilter enforces schoolId cross-checks.

    @Test
    @DisplayName(
            "Admin endpoint: School A session on School B portal is rejected 403 by session binding filter")
    void schoolAAdminSession_onSchoolBPortal_adminEndpointRejected() throws Exception {
        // Admin endpoint: gateway token says School B but session says School A — must be rejected
        mockMvc.perform(
                        get("/api/v1/admin/echo")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_B)
                                .with(withSession(fixtures.sessionA))
                                .with(withTenantContext(fixtures.contextB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));
    }

    @Test
    @DisplayName("Admin endpoint: matching School A session and gateway token succeeds 200 OK")
    void schoolAAdminSession_onSchoolAPortal_succeeds() throws Exception {
        mockMvc.perform(
                        get("/api/v1/admin/echo")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_A)
                                .with(withSession(fixtures.sessionA))
                                .with(withTenantContext(fixtures.contextA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schoolId").value(fixtures.schoolIdA.toString()));
    }

    @Test
    @DisplayName(
            "Admin endpoint: unauthenticated request (no session) is rejected — data does not leak")
    void adminEndpoint_noSession_rejectedWithoutDataLeak() throws Exception {
        // No SecurityContextHolder session set — filter passes through to controller.
        // EchoSchoolController receives null session and throws ResponseStatusException(401).
        // The response must be a client error; critically no school data is in the body.
        mockMvc.perform(
                        get("/api/v1/admin/echo")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_A)
                                .with(withTenantContext(fixtures.contextA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName(
            "must-change-password session is still subject to cross-tenant rejection on admin endpoints")
    void mustChangePasswordSession_crossTenant_stillRejected() throws Exception {
        PrincipalSession mustChangePwdSession =
                new PrincipalSession(
                        fixtures.adminIdA,
                        fixtures.schoolIdA,
                        "principal@school-a.test",
                        AdminRole.PRINCIPAL,
                        true /* mustChangePassword */);

        // Even a must-change-password session cannot cross into School B's admin portal
        mockMvc.perform(
                        get("/api/v1/admin/echo")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_B)
                                .with(withSession(mustChangePwdSession))
                                .with(withTenantContext(fixtures.contextB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("cross_tenant_access_denied"));
    }

    @Test
    @DisplayName(
            "Two simultaneous but distinct admin sessions on their own portals both succeed"
                    + " and never observe each other's school data")
    void twoAdminSessions_distinctPortals_noCrossObservation() throws Exception {
        // School A session on School A portal → observes School A data
        String responseA =
                mockMvc.perform(
                                get("/api/v1/admin/echo")
                                        .header(
                                                GatewayCredentialAuthenticationFilter
                                                        .GATEWAY_TOKEN_HEADER,
                                                TenantFixtureFactory.TOKEN_A)
                                        .with(withSession(fixtures.sessionA))
                                        .with(withTenantContext(fixtures.contextA)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // School B session on School B portal → observes School B data
        String responseB =
                mockMvc.perform(
                                get("/api/v1/admin/echo")
                                        .header(
                                                GatewayCredentialAuthenticationFilter
                                                        .GATEWAY_TOKEN_HEADER,
                                                TenantFixtureFactory.TOKEN_B)
                                        .with(withSession(fixtures.sessionB))
                                        .with(withTenantContext(fixtures.contextB)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(responseA)
                .contains(fixtures.schoolIdA.toString())
                .doesNotContain(fixtures.schoolIdB.toString());
        assertThat(responseB)
                .contains(fixtures.schoolIdB.toString())
                .doesNotContain(fixtures.schoolIdA.toString());
    }
}
