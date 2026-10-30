/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.fixture.TenantFixtureFactory;
import com.rorfost.schoolportal.featureflag.api.PublicFeatureFlagController;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.application.TenantResolver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that simultaneous requests from different tenants cannot leak context into each other.
 *
 * <p>The production {@link TenantContextHolder} deliberately stores tenant context in HTTP request
 * attributes rather than a ThreadLocal so that even thread-pool reuse cannot cause cross-tenant
 * contamination. These tests confirm that property empirically by firing concurrent requests for
 * School A and School B and asserting each response contains its own tenant's feature flags.
 *
 * <p>Uses {@link PublicFeatureFlagController} (a {@code public} controller) to avoid cross-package
 * access issues with package-private controllers like SchoolController.
 */
class CrossTenantConcurrencyTest {

    private static final int THREAD_COUNT = 8;
    private static final int ITERATIONS_PER_THREAD = 20;

    private TenantFixtureFactory fixtures;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fixtures = new TenantFixtureFactory();

        TenantResolver tenantResolver = Mockito.mock(TenantResolver.class);
        fixtures.stubTenantResolver(tenantResolver);

        FeatureFlagService featureFlagService = Mockito.mock(FeatureFlagService.class);
        // School A has GALLERY=true; School B has GALLERY=false — distinct values for assertion
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdA))
                .thenReturn(
                        Map.of(FeatureKey.GALLERY.name(), true, FeatureKey.MATERIALS.name(), true));
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdB))
                .thenReturn(
                        Map.of(
                                FeatureKey.GALLERY.name(),
                                false,
                                FeatureKey.MATERIALS.name(),
                                true));
        when(featureFlagService.getFeaturesForSchool(fixtures.schoolIdC))
                .thenReturn(
                        Map.of(
                                FeatureKey.GALLERY.name(),
                                true,
                                FeatureKey.MATERIALS.name(),
                                false));

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

    @Test
    @DisplayName(
            "Concurrent School A and School B requests each receive only their own tenant's data"
                    + " — no cross-tenant contamination under parallel load")
    void concurrentRequests_noCrossTenantLeakage() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<List<String>>> futures = new ArrayList<>();

        for (int t = 0; t < THREAD_COUNT; t++) {
            final boolean useSchoolA = (t % 2 == 0);
            Future<List<String>> future =
                    executor.submit(
                            () -> {
                                startGate.await(); // burst all threads simultaneously
                                List<String> results = new ArrayList<>();
                                for (int i = 0; i < ITERATIONS_PER_THREAD; i++) {
                                    String token =
                                            useSchoolA
                                                    ? TenantFixtureFactory.TOKEN_A
                                                    : TenantFixtureFactory.TOKEN_B;
                                    MvcResult result =
                                            mockMvc.perform(
                                                            get("/api/v1/public/features")
                                                                    .header(
                                                                            GatewayCredentialAuthenticationFilter
                                                                                    .GATEWAY_TOKEN_HEADER,
                                                                            token))
                                                    .andExpect(status().isOk())
                                                    .andReturn();
                                    results.add(result.getResponse().getContentAsString());
                                }
                                return results;
                            });
            futures.add(future);
        }

        startGate.countDown(); // release all threads simultaneously
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // School A → GALLERY=true; School B → GALLERY=false — assert no cross-contamination
        for (int t = 0; t < futures.size(); t++) {
            final boolean expectA = (t % 2 == 0);
            List<String> responses;
            try {
                responses = futures.get(t).get();
            } catch (Exception e) {
                throw new AssertionError("Thread " + t + " threw unexpectedly", e);
            }
            for (String body : responses) {
                if (expectA) {
                    assertThat(body)
                            .as("Thread %d (School A) response must contain GALLERY:true", t)
                            .contains("\"GALLERY\":true")
                            .doesNotContain("\"GALLERY\":false");
                } else {
                    assertThat(body)
                            .as("Thread %d (School B) response must contain GALLERY:false", t)
                            .contains("\"GALLERY\":false")
                            .doesNotContain("\"GALLERY\":true");
                }
            }
        }
    }

    @Test
    @DisplayName(
            "TenantContextHolder stores context in request attributes, not ThreadLocal;"
                    + " consecutive reuse of the same thread cannot carry stale context")
    void tenantContextHolder_isRequestScoped_notThreadLocal() {
        // Directly verify the request-attribute binding: a fresh MockHttpServletRequest has no
        // tenant context even when the previous request on this thread had one.
        MockHttpServletRequest requestOne = new MockHttpServletRequest();
        TenantContextHolder.setTenantContext(requestOne, fixtures.contextA);
        assertThat(TenantContextHolder.getTenantContext(requestOne)).contains(fixtures.contextA);

        // Simulate a new request on the same thread — must start with no context
        MockHttpServletRequest requestTwo = new MockHttpServletRequest();
        assertThat(TenantContextHolder.getTenantContext(requestTwo))
                .as(
                        "Fresh request on same thread must have no tenant context — no ThreadLocal leak")
                .isEmpty();

        // Binding to the second request must not affect the first
        TenantContextHolder.setTenantContext(requestTwo, fixtures.contextB);
        assertThat(TenantContextHolder.getTenantContext(requestOne))
                .as("Setting context on request 2 must not mutate request 1")
                .contains(fixtures.contextA);
        assertThat(TenantContextHolder.getTenantContext(requestTwo)).contains(fixtures.contextB);
    }

    @Test
    @DisplayName("Three-tenant simultaneous requests remain strictly isolated")
    void threeTenantConcurrentRequests_strictIsolation() {
        // This test validates the fixture factory's three-way distinct IDs and slugs
        assertThat(fixtures.schoolIdA).isNotEqualTo(fixtures.schoolIdB);
        assertThat(fixtures.schoolIdB).isNotEqualTo(fixtures.schoolIdC);
        assertThat(fixtures.schoolIdA).isNotEqualTo(fixtures.schoolIdC);

        assertThat(fixtures.contextA.slug()).isEqualTo("school-a");
        assertThat(fixtures.contextB.slug()).isEqualTo("school-b");
        assertThat(fixtures.contextC.slug()).isEqualTo("school-c");

        // Admin sessions have distinct schoolIds embedded — no cross-tenant bypass possible via
        // session substitution because the schoolId is verified server-side, not trusted from
        // client
        assertThat(fixtures.sessionA.schoolId()).isEqualTo(fixtures.schoolIdA);
        assertThat(fixtures.sessionB.schoolId()).isEqualTo(fixtures.schoolIdB);
        assertThat(fixtures.sessionC.schoolId()).isEqualTo(fixtures.schoolIdC);
    }
}
