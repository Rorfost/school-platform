package com.rorfost.schoolportal.featureflag.application;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service providing feature flag resolution and operator management.
 *
 * <p>Evaluation rule: If an explicit per-school override exists in {@code school_features}, its
 * value is used. Otherwise, the system falls back to the default enablement policy defined in
 * {@link FeatureKey}.
 */
@Service
@Transactional(readOnly = true)
public class FeatureFlagService {

    private final SchoolFeatureRepository schoolFeatureRepository;
    private final SchoolRepository schoolRepository;
    private final AuditLogService auditLogService;

    public FeatureFlagService(SchoolFeatureRepository schoolFeatureRepository) {
        this(schoolFeatureRepository, null, null);
    }

    public FeatureFlagService(
            SchoolFeatureRepository schoolFeatureRepository,
            SchoolRepository schoolRepository,
            AuditLogService auditLogService) {
        this.schoolFeatureRepository = schoolFeatureRepository;
        this.schoolRepository = schoolRepository;
        this.auditLogService = auditLogService;
    }

    /** Checks if a specific feature is enabled for the specified school. */
    public boolean isFeatureEnabled(UUID schoolId, FeatureKey featureKey) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(featureKey, "featureKey must not be null");

        return schoolFeatureRepository
                .findByIdSchoolIdAndIdFeatureKey(schoolId, featureKey.name())
                .map(SchoolFeature::isEnabled)
                .orElseGet(featureKey::isDefaultEnabled);
    }

    /**
     * Returns a complete map of all registered features and their resolved enablement state for a
     * school.
     */
    public Map<String, Boolean> getFeaturesForSchool(UUID schoolId) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");

        // Load all explicit overrides for this school
        List<SchoolFeature> overrides = schoolFeatureRepository.findByIdSchoolId(schoolId);
        Map<String, Boolean> overrideMap =
                overrides.stream()
                        .collect(
                                Collectors.toMap(
                                        SchoolFeature::getFeatureKey, SchoolFeature::isEnabled));

        Map<String, Boolean> result = new LinkedHashMap<>();

        // Start with platform defaults and apply any existing overrides
        for (FeatureKey key : FeatureKey.values()) {
            boolean enabled = overrideMap.getOrDefault(key.name(), key.isDefaultEnabled());
            result.put(key.name(), enabled);
        }

        return Collections.unmodifiableMap(result);
    }

    /** Operator-level method to set an explicit feature toggle for a school by schoolId. */
    @Transactional
    public void setSchoolFeature(UUID schoolId, FeatureKey featureKey, boolean enabled) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(featureKey, "featureKey must not be null");

        // Validate school exists if repository is available
        if (schoolRepository != null && !schoolRepository.existsById(schoolId)) {
            throw new DomainException(HttpStatus.NOT_FOUND, "school_not_found");
        }

        Optional<SchoolFeature> existing =
                schoolFeatureRepository.findByIdSchoolIdAndIdFeatureKey(
                        schoolId, featureKey.name());

        if (existing.isPresent()) {
            SchoolFeature sf = existing.get();
            sf.setEnabled(enabled);
            schoolFeatureRepository.save(sf);
        } else {
            schoolFeatureRepository.save(new SchoolFeature(schoolId, featureKey, enabled));
        }

        if (auditLogService != null) {
            auditLogService.record(
                    schoolId,
                    null,
                    AuditAction.SCHOOL_FEATURE_UPDATED,
                    "SCHOOL_FEATURE",
                    schoolId,
                    "OPERATOR_CLI");
        }
    }

    /**
     * Safe operator-only method to set an explicit feature toggle for a school by school slug and feature key string.
     * Validates both school slug and feature key, and records an audit log entry.
     */
    @Transactional
    public void setSchoolFeatureBySlug(String schoolSlug, String featureKeyStr, boolean enabled) {
        if (schoolSlug == null || schoolSlug.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "school_slug_required");
        }
        if (featureKeyStr == null || featureKeyStr.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "feature_key_required");
        }

        String normalizedSlug = schoolSlug.strip().toLowerCase(java.util.Locale.ROOT);
        School school =
                schoolRepository
                        .findBySlug(normalizedSlug)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));

        FeatureKey featureKey =
                FeatureKey.fromString(featureKeyStr)
                        .orElseThrow(
                                () -> new DomainException(HttpStatus.BAD_REQUEST, "invalid_feature_key"));

        setSchoolFeature(school.getId(), featureKey, enabled);
    }
}
