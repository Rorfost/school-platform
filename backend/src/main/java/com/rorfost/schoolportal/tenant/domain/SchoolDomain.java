package com.rorfost.schoolportal.tenant.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Maps an allowed operator-configured hostname (e.g. Worker domain or custom domain) to a specific
 * school tenant.
 */
@Entity
@Table(name = "school_domains")
public class SchoolDomain extends AuditableUuidEntity {

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "hostname", nullable = false, unique = true)
    private String hostname;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    protected SchoolDomain() {}

    public SchoolDomain(UUID schoolId, String hostname, boolean isPrimary) {
        this.schoolId = Objects.requireNonNull(schoolId, "schoolId must not be null");
        this.hostname = normalizeHostname(hostname);
        this.isPrimary = isPrimary;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public String getHostname() {
        return hostname;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public static String normalizeHostname(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("hostname must not be blank");
        }
        String clean = hostname.strip().toLowerCase(Locale.ROOT);
        if (clean.startsWith("http://")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("https://")) {
            clean = clean.substring(8);
        }
        int slashIndex = clean.indexOf('/');
        if (slashIndex != -1) {
            clean = clean.substring(0, slashIndex);
        }
        int colonIndex = clean.indexOf(':');
        if (colonIndex != -1) {
            clean = clean.substring(0, colonIndex);
        }
        if (clean.length() < 3) {
            throw new IllegalArgumentException("hostname is invalid or too short: " + hostname);
        }
        return clean;
    }
}
