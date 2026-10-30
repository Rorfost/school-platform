package com.rorfost.schoolportal.featureflag.application;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/**
 * Enforcement guard for platform features.
 *
 * <p>Enforces stable error conventions when a feature is disabled for a school:
 *
 * <ul>
 *   <li>Public endpoints fail with 404 NOT_FOUND ("feature_not_found") so disabled features cannot
 *       be probed.
 *   <li>Administrative endpoints fail with 403 FORBIDDEN ("feature_disabled") providing clear
 *       feedback to authenticated staff.
 * </ul>
 */
@Component
public class FeatureAccessPolicy {

    private final FeatureFlagService featureFlagService;

    public FeatureAccessPolicy(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    /**
     * Enforces that the feature is enabled for public access on the given school. Fails closed with
     * 404 NOT_FOUND if disabled.
     */
    public void requirePublicEnabled(UUID schoolId, FeatureKey featureKey) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(featureKey, "featureKey must not be null");

        if (!featureFlagService.isFeatureEnabled(schoolId, featureKey)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "feature_not_found");
        }
    }

    /**
     * Enforces that the feature is enabled for administrative management on the given school. Fails
     * closed with 403 FORBIDDEN if disabled.
     */
    public void requireAdminEnabled(UUID schoolId, FeatureKey featureKey) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(featureKey, "featureKey must not be null");

        if (!featureFlagService.isFeatureEnabled(schoolId, featureKey)) {
            throw new DomainException(HttpStatus.FORBIDDEN, "feature_disabled");
        }
    }
}
