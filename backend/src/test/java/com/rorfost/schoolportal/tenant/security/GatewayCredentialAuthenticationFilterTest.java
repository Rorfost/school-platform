package com.rorfost.schoolportal.tenant.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class GatewayCredentialAuthenticationFilterTest {

    private TenantResolver tenantResolver;
    private ObjectMapper objectMapper;
    private GatewayCredentialAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tenantResolver = mock(TenantResolver.class);
        objectMapper = new ObjectMapper();
        filter = new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void doFilter_shouldAuthenticateAndBindTenantContextForSchoolA()
            throws ServletException, IOException {
        String tokenA = "worker-secret-school-a";
        UUID schoolIdA = UUID.randomUUID();
        TenantContext contextA = new TenantContext(schoolIdA, "school-a");

        when(tenantResolver.resolveTenant(tokenA)).thenReturn(Optional.of(contextA));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/school");
        request.addHeader(GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER, tokenA);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);

        Optional<TenantContext> bound = TenantContextHolder.getTenantContext(request);
        assertThat(bound).isPresent();
        assertThat(bound.get().schoolId()).isEqualTo(schoolIdA);
        assertThat(bound.get().slug()).isEqualTo("school-a");
    }

    @Test
    void doFilter_shouldAuthenticateAndBindTenantContextForSchoolB()
            throws ServletException, IOException {
        String tokenB = "worker-secret-school-b";
        UUID schoolIdB = UUID.randomUUID();
        TenantContext contextB = new TenantContext(schoolIdB, "school-b");

        when(tenantResolver.resolveTenant(tokenB)).thenReturn(Optional.of(contextB));

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/notices");
        request.addHeader(GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER, tokenB);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);

        Optional<TenantContext> bound = TenantContextHolder.getTenantContext(request);
        assertThat(bound).isPresent();
        assertThat(bound.get().schoolId()).isEqualTo(schoolIdB);
        assertThat(bound.get().slug()).isEqualTo("school-b");
    }

    @Test
    void doFilter_shouldRejectMissingTokenWith403Forbidden() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/school");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("missing_gateway_credential");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_shouldRejectInvalidOrRevokedTokenWith403Forbidden()
            throws ServletException, IOException {
        String invalidToken = "bad-or-revoked-token";
        when(tenantResolver.resolveTenant(invalidToken)).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/school");
        request.addHeader(GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER, invalidToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("invalid_gateway_credential");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_shouldIgnoreMaliciousClientForgedSlugAndDeriveFromToken()
            throws ServletException, IOException {
        String tokenA = "worker-secret-school-a";
        UUID schoolIdA = UUID.randomUUID();
        TenantContext contextA = new TenantContext(schoolIdA, "school-a");

        when(tenantResolver.resolveTenant(tokenA)).thenReturn(Optional.of(contextA));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/school");
        request.addHeader(GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER, tokenA);
        request.addHeader("X-Portal-School-Slug", "attacker-forged-school-slug");
        request.setParameter("schoolId", UUID.randomUUID().toString());

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Optional<TenantContext> bound = TenantContextHolder.getTenantContext(request);
        assertThat(bound).isPresent();
        assertThat(bound.get().slug()).isEqualTo("school-a");
        assertThat(bound.get().slug()).isNotEqualTo("attacker-forged-school-slug");
    }

    @Test
    void shouldNotFilter_shouldAllowActuatorHealthWithoutToken()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(TenantContextHolder.getTenantContext(request)).isEmpty();
    }

    @Test
    void shouldNotFilter_shouldAllowCorsPreflightOptionsRequests()
            throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("OPTIONS", "/api/v1/public/school");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilter_shouldBypassWhenGatewayDisabledInTransitionalMode()
            throws ServletException, IOException {
        GatewayCredentialAuthenticationFilter disabledFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/school");
        MockHttpServletResponse response = new MockHttpServletResponse();

        disabledFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void concurrentRequests_shouldMaintainStrictTenantContextIsolation()
            throws ExecutionException, InterruptedException {
        String tokenA = "token-concurrent-a";
        String tokenB = "token-concurrent-b";

        UUID schoolIdA = UUID.randomUUID();
        UUID schoolIdB = UUID.randomUUID();

        TenantContext contextA = new TenantContext(schoolIdA, "school-a");
        TenantContext contextB = new TenantContext(schoolIdB, "school-b");

        when(tenantResolver.resolveTenant(tokenA)).thenReturn(Optional.of(contextA));
        when(tenantResolver.resolveTenant(tokenB)).thenReturn(Optional.of(contextB));

        CompletableFuture<TenantContext> taskA =
                CompletableFuture.supplyAsync(
                        () -> {
                            MockHttpServletRequest req =
                                    new MockHttpServletRequest("GET", "/api/v1/public/school");
                            req.addHeader(
                                    GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                    tokenA);
                            try {
                                filter.doFilter(
                                        req,
                                        new MockHttpServletResponse(),
                                        mock(FilterChain.class));
                                return TenantContextHolder.getTenantContext(req).orElseThrow();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        CompletableFuture<TenantContext> taskB =
                CompletableFuture.supplyAsync(
                        () -> {
                            MockHttpServletRequest req =
                                    new MockHttpServletRequest("GET", "/api/v1/public/school");
                            req.addHeader(
                                    GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                    tokenB);
                            try {
                                filter.doFilter(
                                        req,
                                        new MockHttpServletResponse(),
                                        mock(FilterChain.class));
                                return TenantContextHolder.getTenantContext(req).orElseThrow();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        TenantContext resultA = taskA.get();
        TenantContext resultB = taskB.get();

        assertThat(resultA.schoolId()).isEqualTo(schoolIdA);
        assertThat(resultA.slug()).isEqualTo("school-a");

        assertThat(resultB.schoolId()).isEqualTo(schoolIdB);
        assertThat(resultB.slug()).isEqualTo("school-b");

        assertThat(resultA.schoolId()).isNotEqualTo(resultB.schoolId());
    }
}
