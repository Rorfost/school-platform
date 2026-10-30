package com.rorfost.schoolportal.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Filter enforcing strict tenant binding on every administrative endpoint.
 *
 * <p>Validates that whenever an authenticated {@link PrincipalSession} accesses {@code
 * /api/v1/admin/**}, the principal's {@code schoolId} matches the verified request {@link
 * TenantContext#schoolId()}. Cross-tenant tampering is denied immediately with HTTP 403 Forbidden
 * and code {@code cross_tenant_access_denied}; sessions are never silently reassigned.
 *
 * <p>For security-sensitive mutations, it verifies that the principal account remains active in the
 * database.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class AdminSessionTenantBindingFilter extends OncePerRequestFilter {

    private static final Set<String> UNPROTECTED_ADMIN_AUTH_PATHS =
            Set.of(
                    "/api/v1/admin/auth/csrf",
                    "/api/v1/admin/auth/login",
                    "/api/v1/admin/auth/logout");

    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;

    public AdminSessionTenantBindingFilter(
            AdminUserRepository adminUserRepository, ObjectMapper objectMapper) {
        this.adminUserRepository = adminUserRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/admin/")) {
            return true;
        }

        // CSRF retrieval, login, and logout manage/establish/clear sessions rather than requiring
        // an
        // existing matching authenticated session.
        return UNPROTECTED_ADMIN_AUTH_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof PrincipalSession principal)) {
            // Unauthenticated requests are delegated to Spring Security's SecurityFilterChain
            // authorization checks to yield standard 401 unauthenticated problem responses.
            filterChain.doFilter(request, response);
            return;
        }

        Optional<TenantContext> maybeTenant =
                TenantContextHolder.getTenantContext(request)
                        .or(TenantContextHolder::getCurrentTenant);

        if (maybeTenant.isEmpty()) {
            reject(request, response, HttpStatus.FORBIDDEN, "missing_tenant_context");
            return;
        }

        TenantContext tenant = maybeTenant.get();
        if (!tenant.schoolId().equals(principal.schoolId())) {
            reject(request, response, HttpStatus.FORBIDDEN, "cross_tenant_access_denied");
            return;
        }

        // Re-check principal account active state for security-sensitive state mutations
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)) {
            boolean active =
                    adminUserRepository
                            .findById(principal.adminUserId())
                            .filter(AdminUser::isActive)
                            .filter(user -> tenant.schoolId().equals(user.getSchoolId()))
                            .isPresent();
            if (!active) {
                reject(request, response, HttpStatus.FORBIDDEN, "account_inactive_or_revoked");
                return;
            }
        }

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
