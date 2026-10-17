/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.fixture.TenantFixtureFactory;
import com.rorfost.schoolportal.featureflag.api.PublicFeatureFlagController;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.tenant.application.TenantResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

/**
 * Negative security tests for header and origin bypass attacks against the gateway filter.
 *
 * <p>Uses {@link PublicFeatureFlagController} (a {@code public} controller accessible across
 * packages) as the test endpoint. The gateway filter behavior under attack is controller-agnostic:
 * all rejections happen before the controller is invoked.
 *
 * <p>Data-isolation tests use distinct per-school feature flag maps (School A → GALLERY=true,
 * School B → GALLERY=false, School C → EXAM_RESULTS=false) as distinguishable fingerprints instead
 * of school names, since the school controller is package-private.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Direct-to-origin request with no gateway token (simulates bypassing Cloudflare Worker).
 *   <li>Forged or spoofed {@code X-Portal-Gateway-Token} header values.
 *   <li>Request with Token A but injected School B slug/ID parameters — still returns School A
 *       features (tenant derived from verified token, never from client params).
 *   <li>Token for School C with School A ID in query param — still returns School C features.
 *   <li>Empty or blank token values are rejected 403 with the correct error code.
 * </ul>
 */
class ForgedHeaderRequestTest {

    private TenantFixtureFactory fixtures;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fixtures = new TenantFixtureFactory();

        TenantResolver tenantResolver = Mockito.mock(TenantResolver.class);
        fixtures.stubTenantResolver(tenantResolver);

        FeatureFlagService featureFlagService = Mockito.mock(FeatureFlagService.class);
        // Distinct feature maps per school — used as distinguishable tenant fingerprints
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdA))
                .thenReturn(
                        Map.of(
                                FeatureKey.GALLERY.name(), true,
                                FeatureKey.MATERIALS.name(), true,
                                FeatureKey.EXAM_RESULTS.name(), true));
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdB))
                .thenReturn(
                        Map.of(
                                FeatureKey.GALLERY.name(), false,
                                FeatureKey.MATERIALS.name(), true,
                                FeatureKey.EXAM_RESULTS.name(), true));
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdC))
                .thenReturn(
                        Map.of(
                                FeatureKey.GALLERY.name(), true,
                                FeatureKey.MATERIALS.name(), true,
                                FeatureKey.EXAM_RESULTS.name(), false));

        PublicFeatureFlagController controller =
                new PublicFeatureFlagController(featureFlagService);
        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, new ObjectMapper(), true);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();
    }

    // ── Missing / empty token (simulates direct-to-Render origin bypass attempt) ────────────────

    @Test
    @DisplayName(
            "Direct-to-Render request with no gateway token is rejected 403"
                    + " missing_gateway_credential")
    void directToOrigin_noToken_rejected() throws Exception {
        // Simulates attacker calling the Render backend URL directly without going through the
        // Cloudflare Worker that injects the gateway token.
        mockMvc.perform(get("/api/v1/public/features"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));
    }

    @Test
    @DisplayName("Blank/whitespace token value is rejected 403 missing_gateway_credential")
    void blankToken_rejected() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "   "))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));
    }

    // ── Forged / unknown token values
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Completely forged gateway token is rejected 403 invalid_gateway_credential")
    void forgedToken_rejected() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "totally-forged-credential"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Unknown (unregistered) token is rejected 403 invalid_gateway_credential")
    void unknownToken_rejected() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_UNKNOWN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Revoked/inactive tenant token is rejected 403 invalid_gateway_credential")
    void inactiveTenantToken_rejected() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_INACTIVE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    // ── Data isolation: token is the authority, not query params
    // ──────────────────────────────────

    @Test
    @DisplayName(
            "School C token with School A's schoolId in query param returns School C features"
                    + " — query param is ignored, token is the authority")
    void schoolCToken_withSchoolAIdParam_returnsSchoolCFeatures() throws Exception {
        // School C fingerprint: EXAM_RESULTS=false; School A: EXAM_RESULTS=true
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_C)
                                // Attacker injects School A's ID in query param — must be ignored
                                .param("schoolId", fixtures.schoolIdA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.EXAM_RESULTS").value(false))
                // School A has EXAM_RESULTS=true; if we see false, it means School C context was
                // used
                .andExpect(jsonPath("$.GALLERY").value(true));
    }

    @Test
    @DisplayName(
            "Token A + injected School B slug in query param and header returns School A features"
                    + " — tenant derived from token, never from client params")
    void tokenA_withSpoofedSchoolBParams_returnsSchoolAFeatures() throws Exception {
        // School A fingerprint: GALLERY=true; School B: GALLERY=false
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_A)
                                // Attacker spoofs both slug and schoolId query params
                                .param("slug", "school-b")
                                .param("schoolId", fixtures.schoolIdB.toString())
                                .header("X-Portal-School-Slug", "school-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GALLERY").value(true)); // School A's value
    }

    @Test
    @DisplayName("Token B + injected School A schoolId in query param returns School B features")
    void tokenB_withSpoofedSchoolAId_returnsSchoolBFeatures() throws Exception {
        // School B fingerprint: GALLERY=false; School A: GALLERY=true
        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_B)
                                .param("schoolId", fixtures.schoolIdA.toString())
                                .param("id", fixtures.schoolIdA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GALLERY").value(false)); // School B's value
    }

    // ── Health and non-API paths bypass the gateway filter
    // ────────────────────────────────────────

    @Test
    @DisplayName("Actuator health endpoint is reachable without gateway token")
    void actuatorHealth_noToken_allowed() throws Exception {
        // The gateway filter explicitly skips /actuator/health paths.
        // MockMvc won't have an actuator endpoint mapped in standalone setup, so the request will
        // 404 — but it must NOT return 403 (i.e., the filter must have skipped the path).
        int statusCode =
                mockMvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();

        // 404 is acceptable; 403 would indicate the filter erroneously blocked the health endpoint.
        assertThat(statusCode).isNotEqualTo(403);
    }
}
