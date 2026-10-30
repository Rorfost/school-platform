package com.rorfost.schoolportal.tenant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

class SchoolGatewayCredentialTest {

    private static final String TEST_TOKEN = "cf-worker-token-test-secret-value-32bytes-long!!";

    @Test
    void shouldConstructValidCredential() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);
        Instant futureExpiry = Instant.now().plus(90, ChronoUnit.DAYS);

        SchoolGatewayCredential cred =
                new SchoolGatewayCredential(
                        schoolId, digest, "Worker-SchoolA-Primary", futureExpiry);

        assertThat(cred.getSchoolId()).isEqualTo(schoolId);
        assertThat(cred.getTokenDigest()).isEqualTo(digest);
        assertThat(cred.getLabel()).isEqualTo("Worker-SchoolA-Primary");
        assertThat(cred.getExpiresAt()).isEqualTo(futureExpiry);
        assertThat(cred.getCreatedAt()).isNotNull();
        assertThat(cred.getRevokedAt()).isNull();
        assertThat(cred.getLastUsedAt()).isNull();
        assertThat(cred.getRotatedAt()).isNull();
        assertThat(cred.isActive()).isTrue();
    }

    @Test
    void shouldRejectNullSchoolId() {
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);

        assertThatThrownBy(() -> new SchoolGatewayCredential(null, digest, "Worker-Label"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("schoolId");
    }

    @Test
    void shouldRejectInvalidDigest() {
        UUID schoolId = UUID.randomUUID();

        assertThatThrownBy(() -> new SchoolGatewayCredential(schoolId, null, "Worker-Label"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenDigest must be a valid 32-byte SHA-256 hash");

        byte[] shortDigest = new byte[16];
        assertThatThrownBy(() -> new SchoolGatewayCredential(schoolId, shortDigest, "Worker-Label"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenDigest must be a valid 32-byte SHA-256 hash");
    }

    @Test
    void shouldRejectBlankLabel() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);

        assertThatThrownBy(() -> new SchoolGatewayCredential(schoolId, digest, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label must not be blank");
    }

    @Test
    void shouldRejectExpiryBeforeCreation() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(
                        () -> new SchoolGatewayCredential(schoolId, digest, "Worker-Label", past))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt must be after createdAt");
    }

    @Test
    void shouldEvaluateIsActiveCorrectly() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);
        Instant now = Instant.now();
        Instant futureExpiry = now.plus(30, ChronoUnit.DAYS);

        SchoolGatewayCredential cred =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Active", futureExpiry);

        assertThat(cred.isActive(now)).isTrue();

        // Expired check
        Instant afterExpiry = futureExpiry.plus(1, ChronoUnit.SECONDS);
        assertThat(cred.isActive(afterExpiry)).isFalse();

        // Revocation check
        cred.revoke(now);
        assertThat(cred.getRevokedAt()).isEqualTo(now);
        assertThat(cred.isActive(now)).isFalse();
    }

    @Test
    void shouldRecordUsageAndRotation() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest(TEST_TOKEN);
        SchoolGatewayCredential cred =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Rotation");

        Instant usedAt = Instant.now();
        cred.markUsed(usedAt);
        assertThat(cred.getLastUsedAt()).isEqualTo(usedAt);

        Instant rotatedAt = usedAt.plus(10, ChronoUnit.MINUTES);
        cred.recordRotation(rotatedAt);
        assertThat(cred.getRotatedAt()).isEqualTo(rotatedAt);
    }

    @Test
    void computeDigest_shouldProduceDeterministic32ByteHash() {
        byte[] hash1 = SchoolGatewayCredential.computeDigest("secret-token-value");
        byte[] hash2 = SchoolGatewayCredential.computeDigest("secret-token-value");

        assertThat(hash1).hasSize(32);
        assertThat(hash1).isEqualTo(hash2);

        byte[] differentHash = SchoolGatewayCredential.computeDigest("different-token-value");
        assertThat(hash1).isNotEqualTo(differentHash);
    }

    @Test
    void computeDigest_shouldThrowOnEmptyInput() {
        assertThatThrownBy(() -> SchoolGatewayCredential.computeDigest(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolGatewayCredential.computeDigest("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SchoolGatewayCredential.computeDigest((String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getTokenDigest_shouldReturnDefensiveCopy() {
        UUID schoolId = UUID.randomUUID();
        byte[] original = SchoolGatewayCredential.computeDigest(TEST_TOKEN);
        SchoolGatewayCredential cred =
                new SchoolGatewayCredential(schoolId, original, "Defensive-Test");

        byte[] returned = cred.getTokenDigest();
        returned[0] ^= 0xFF; // Mutate copy

        assertThat(cred.getTokenDigest()).isEqualTo(original);
    }
}
