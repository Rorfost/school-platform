package com.rorfost.schoolportal.featureflag.application;

import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;

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

    public FeatureFlagService(SchoolFeatureRepository schoolFeatureRepository) {
        this.schoolFeatureRepository = schoolFeatureRepository;
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

    /** Operator-level method to set an explicit feature toggle for a school. */
    @Transactional
    public void setSchoolFeature(UUID schoolId, FeatureKey featureKey, boolean enabled) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(featureKey, "featureKey must not be null");

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
    }
}
