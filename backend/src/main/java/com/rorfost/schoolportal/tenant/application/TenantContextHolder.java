package com.rorfost.schoolportal.tenant.application;

import com.rorfost.schoolportal.tenant.domain.TenantContext;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Request-scoped accessor for {@link TenantContext}.
 *
 * <p>Uses {@link HttpServletRequest} attributes to guarantee strict request isolation and eliminate
 * thread-pool state leakage or mutable singleton concurrency issues.
 */
public final class TenantContextHolder {

    private TenantContextHolder() {}

    /**
     * Binds the verified tenant context to the specified HTTP servlet request.
     *
     * @param request the current HTTP servlet request
     * @param tenantContext the verified tenant context to bind
     */
    public static void setTenantContext(HttpServletRequest request, TenantContext tenantContext) {
        if (request != null && tenantContext != null) {
            request.setAttribute(TenantContext.ATTRIBUTE_NAME, tenantContext);
        }
    }

    /**
     * Retrieves the tenant context bound to the specified request.
     *
     * @param request the current HTTP servlet request
     * @return an {@link Optional} containing the tenant context, or empty if not present
     */
    public static Optional<TenantContext> getTenantContext(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        Object attr = request.getAttribute(TenantContext.ATTRIBUTE_NAME);
        if (attr instanceof TenantContext context) {
            return Optional.of(context);
        }
        return Optional.empty();
    }

    /**
     * Resolves the current tenant context from the thread-bound {@link RequestContextHolder}.
     *
     * @return an {@link Optional} containing the current tenant context, or empty if absent
     */
    public static Optional<TenantContext> getCurrentTenant() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return getTenantContext(servletRequestAttributes.getRequest());
        }
        return Optional.empty();
    }

    /**
     * Resolves the current tenant context or throws {@link IllegalStateException} if absent.
     *
     * @return the verified active tenant context
     * @throws IllegalStateException if called outside a verified tenant request scope
     */
    public static TenantContext requireCurrentTenant() {
        return getCurrentTenant()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No active TenantContext associated with current request"));
    }
}
