package com.rorfost.schoolportal.featureflag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class SchoolFeatureId implements Serializable {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "feature_key", nullable = false, length = 64)
    private String featureKey;

    protected SchoolFeatureId() {}

    public SchoolFeatureId(UUID schoolId, String featureKey) {
        this.schoolId = Objects.requireNonNull(schoolId, "schoolId must not be null");
        if (featureKey == null || featureKey.isBlank()) {
            throw new IllegalArgumentException("featureKey must not be blank");
        }
        this.featureKey = featureKey.strip().toUpperCase();
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchoolFeatureId that = (SchoolFeatureId) o;
        return Objects.equals(schoolId, that.schoolId)
                && Objects.equals(featureKey, that.featureKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schoolId, featureKey);
    }
}
