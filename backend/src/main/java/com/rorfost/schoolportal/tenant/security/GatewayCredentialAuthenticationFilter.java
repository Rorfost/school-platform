package com.rorfost.schoolportal.tenant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Filter intercepting API requests to enforce cryptographic gateway token authentication.
 *
 * <p>Validates {@code X-Portal-Gateway-Token}, resolves the active tenant via {@link
 * TenantResolver}, binds the immutable {@link TenantContext} to the request attributes, and fails
 * closed with 403 Forbidden if the credential is missing, invalid, expired, revoked, or mapped to
 * an inactive school.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class GatewayCredentialAuthenticationFilter extends OncePerRequestFilter {

    public static final String GATEWAY_TOKEN_HEADER = "X-Portal-Gateway-Token";

    private final TenantResolver tenantResolver;
    private final ObjectMapper objectMapper;
    private final boolean gatewayEnabled;

    public GatewayCredentialAuthenticationFilter(
            TenantResolver tenantResolver,
            ObjectMapper objectMapper,
            @Value("${app.gateway.enabled:false}") boolean gatewayEnabled) {
        this.tenantResolver = tenantResolver;
        this.objectMapper = objectMapper;
        this.gatewayEnabled = gatewayEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight CORS requests do not require gateway authentication
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // Allow minimal anonymous health check endpoint
        if (path.equals("/actuator/health") || path.startsWith("/actuator/health/")) {
            return true;
        }

        // Only enforce gateway tokens on /api/* routes
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // When transitional gateway gate is disabled, proceed without blocking
        if (!gatewayEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(GATEWAY_TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            reject(request, response, HttpStatus.FORBIDDEN, "missing_gateway_credential");
            return;
        }

        Optional<TenantContext> maybeTenant = tenantResolver.resolveTenant(token);
        if (maybeTenant.isEmpty()) {
            reject(request, response, HttpStatus.FORBIDDEN, "invalid_gateway_credential");
            return;
        }

        TenantContext tenantContext = maybeTenant.get();
        TenantContextHolder.setTenantContext(request, tenantContext);

        filterChain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "about:blank");
        problem.put("title", status.getReasonPhrase());
        problem.put("status", status.value());
        problem.put("detail", "Request failed tenant authorization.");
        problem.put("instance", URI.create(request.getRequestURI()));
        problem.put("code", code);
        problem.put("requestId", MDC.get("requestId"));

        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
