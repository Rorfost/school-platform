package com.rorfost.schoolportal.featureflag.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class PublicFeatureFlagTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";

    private FeatureFlagService featureFlagService;
    private TenantResolver tenantResolver;
    private MockMvc mockMvc;

    private final UUID schoolA = UUID.randomUUID();
    private final UUID schoolB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        featureFlagService = Mockito.mock(FeatureFlagService.class);
        tenantResolver = Mockito.mock(TenantResolver.class);
        ObjectMapper objectMapper = new ObjectMapper();

        PublicFeatureFlagController controller =
                new PublicFeatureFlagController(featureFlagService);
        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();

        when(tenantResolver.resolveTenant(TOKEN_A))
                .thenReturn(Optional.of(new TenantContext(schoolA, "school-a")));
        when(tenantResolver.resolveTenant(TOKEN_B))
                .thenReturn(Optional.of(new TenantContext(schoolB, "school-b")));
    }

    @Test
    void publicFeaturesReturnsTenantScopedConfigurationForSchoolA() throws Exception {
        Map<String, Boolean> featuresA =
                Map.of(
                        FeatureKey.GALLERY.name(), true,
                        FeatureKey.MATERIALS.name(), true,
                        FeatureKey.EXAM_RESULTS.name(), true);
        when(featureFlagService.getFeaturesForSchool(schoolA)).thenReturn(featuresA);

        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GALLERY").value(true))
                .andExpect(jsonPath("$.MATERIALS").value(true))
                .andExpect(jsonPath("$.EXAM_RESULTS").value(true));
    }

    @Test
    void publicFeaturesReturnsTenantScopedConfigurationForSchoolB() throws Exception {
        // School B has disabled GALLERY and EXAM_RESULTS
        Map<String, Boolean> featuresB =
                Map.of(
                        FeatureKey.GALLERY.name(), false,
                        FeatureKey.MATERIALS.name(), true,
                        FeatureKey.EXAM_RESULTS.name(), false);
        when(featureFlagService.getFeaturesForSchool(schoolB)).thenReturn(featuresB);

        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.GALLERY").value(false))
                .andExpect(jsonPath("$.MATERIALS").value(true))
                .andExpect(jsonPath("$.EXAM_RESULTS").value(false));
    }

    @Test
    void publicFeaturesFailsClosedWhenGatewayTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/public/features")).andExpect(status().isForbidden());
    }

    @Test
    void publicFeaturesFailsClosedWhenGatewayTokenIsInvalid() throws Exception {
        when(tenantResolver.resolveTenant("unknown-token")).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/public/features")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "unknown-token"))
                .andExpect(status().isForbidden());
    }
}
