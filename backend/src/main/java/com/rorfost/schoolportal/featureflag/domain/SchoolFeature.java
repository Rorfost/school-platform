package com.rorfost.schoolportal.featureflag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Represents an explicit per-school override for a platform feature. */
@Entity
@Table(name = "school_features")
public class SchoolFeature {

    @EmbeddedId private SchoolFeatureId id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SchoolFeature() {}

    public SchoolFeature(UUID schoolId, FeatureKey featureKey, boolean enabled) {
        Objects.requireNonNull(featureKey, "featureKey must not be null");
        this.id = new SchoolFeatureId(schoolId, featureKey.name());
        this.enabled = enabled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public SchoolFeatureId getId() {
        return id;
    }

    public UUID getSchoolId() {
        return id != null ? id.getSchoolId() : null;
    }

    public String getFeatureKey() {
        return id != null ? id.getFeatureKey() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
