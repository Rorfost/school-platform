package com.rorfost.schoolportal.tenant.domain;

import com.rorfost.schoolportal.common.persistence.UuidEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a cryptographic route-credential mapped to a school tenant.
 *
 * <p>Cloudflare Workers inject an opaque, secret gateway token in {@code X-Portal-Gateway-Token}.
 * The backend hashes the received token using SHA-256 and matches against {@link #tokenDigest} to
 * resolve the active {@link #schoolId}. Plaintext tokens are never persisted.
 */
@Entity
@Table(name = "school_gateway_credentials")
public class SchoolGatewayCredential extends UuidEntity {

    public static final int SHA256_DIGEST_LENGTH_BYTES = 32;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "token_digest", nullable = false, unique = true)
    private byte[] tokenDigest;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    protected SchoolGatewayCredential() {}

    public SchoolGatewayCredential(UUID schoolId, byte[] tokenDigest, String label) {
        this(schoolId, tokenDigest, label, null);
    }

    public SchoolGatewayCredential(
            UUID schoolId, byte[] tokenDigest, String label, Instant expiresAt) {
        this.schoolId = Objects.requireNonNull(schoolId, "schoolId must not be null");

        if (tokenDigest == null || tokenDigest.length != SHA256_DIGEST_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "tokenDigest must be a valid 32-byte SHA-256 hash, got "
                            + (tokenDigest == null ? "null" : tokenDigest.length + " bytes"));
        }
        this.tokenDigest = tokenDigest.clone();

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        this.label = label.strip();
        this.createdAt = Instant.now();

        if (expiresAt != null && !expiresAt.isAfter(this.createdAt)) {
            throw new IllegalArgumentException("expiresAt must be after createdAt");
        }
        this.expiresAt = expiresAt;
    }

    /**
     * Determines whether this credential is valid and active at the given timestamp.
     *
     * @param now timestamp against which to check revocation and expiration
     * @return {@code true} if not revoked and not expired; {@code false} otherwise
     */
    public boolean isActive(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    public boolean isActive() {
        return isActive(Instant.now());
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = Objects.requireNonNull(revokedAt, "revokedAt must not be null");
    }

    public void markUsed(Instant usedAt) {
        this.lastUsedAt = Objects.requireNonNull(usedAt, "usedAt must not be null");
    }

    public void recordRotation(Instant rotatedAt) {
        this.rotatedAt = Objects.requireNonNull(rotatedAt, "rotatedAt must not be null");
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public byte[] getTokenDigest() {
        return tokenDigest != null ? tokenDigest.clone() : null;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    /** Computes a 32-byte SHA-256 digest from the given raw token string. */
    public static byte[] computeDigest(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("rawToken must not be blank");
        }
        return computeDigest(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    /** Computes a 32-byte SHA-256 digest from the given raw token bytes. */
    public static byte[] computeDigest(byte[] rawTokenBytes) {
        if (rawTokenBytes == null || rawTokenBytes.length == 0) {
            throw new IllegalArgumentException("rawTokenBytes must not be empty");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(rawTokenBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }
}
