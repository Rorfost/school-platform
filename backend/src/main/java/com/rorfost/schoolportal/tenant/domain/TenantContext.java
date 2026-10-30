package com.rorfost.schoolportal.tenant.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable request-scoped context containing the verified school identity.
 *
 * <p>Constructed exclusively by the tenant gateway resolution layer after validating the
 * cryptographic {@code X-Portal-Gateway-Token}. Controllers and services must read this context
 * rather than trusting user-supplied parameters.
 *
 * @param schoolId unique identifier of the authenticated tenant school
 * @param slug unique URL slug of the authenticated tenant school
 */
public record TenantContext(UUID schoolId, String slug) {

    public static final String ATTRIBUTE_NAME = "com.rorfost.schoolportal.tenant.TenantContext";

    public TenantContext {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (slug.isBlank()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
    }
}
