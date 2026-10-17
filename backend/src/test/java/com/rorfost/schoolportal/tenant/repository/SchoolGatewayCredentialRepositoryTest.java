package com.rorfost.schoolportal.tenant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class SchoolGatewayCredentialRepositoryTest {

    private SchoolGatewayCredentialRepository repository;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SchoolGatewayCredentialRepository.class);
    }

    @Test
    void findByTokenDigest_shouldReturnMatchingCredential() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest("token-abc");
        SchoolGatewayCredential credential =
                new SchoolGatewayCredential(schoolId, digest, "Worker-A");

        when(repository.findByTokenDigest(digest)).thenReturn(Optional.of(credential));

        Optional<SchoolGatewayCredential> result = repository.findByTokenDigest(digest);

        assertThat(result).isPresent();
        assertThat(result.get().getSchoolId()).isEqualTo(schoolId);
        assertThat(result.get().getLabel()).isEqualTo("Worker-A");
        verify(repository).findByTokenDigest(digest);
    }

    @Test
    void findActiveByTokenDigest_shouldReturnActiveCredential() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest = SchoolGatewayCredential.computeDigest("token-active");
        SchoolGatewayCredential credential =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Active");
        Instant now = Instant.now();

        when(repository.findActiveByTokenDigest(digest, now)).thenReturn(Optional.of(credential));

        Optional<SchoolGatewayCredential> result = repository.findActiveByTokenDigest(digest, now);

        assertThat(result).isPresent();
        assertThat(result.get().getLabel()).isEqualTo("Worker-Active");
    }

    @Test
    void countBySchoolIdAndRevokedAtIsNull_shouldReturnActiveCredentialCount() {
        UUID schoolId = UUID.randomUUID();
        when(repository.countBySchoolIdAndRevokedAtIsNull(schoolId)).thenReturn(2L);

        long activeCount = repository.countBySchoolIdAndRevokedAtIsNull(schoolId);

        assertThat(activeCount).isEqualTo(2L);
        verify(repository).countBySchoolIdAndRevokedAtIsNull(schoolId);
    }

    @Test
    void findAllBySchoolId_shouldReturnAllSchoolCredentials() {
        UUID schoolId = UUID.randomUUID();
        byte[] digest1 = SchoolGatewayCredential.computeDigest("token-1");
        byte[] digest2 = SchoolGatewayCredential.computeDigest("token-2");

        SchoolGatewayCredential cred1 = new SchoolGatewayCredential(schoolId, digest1, "Primary");
        SchoolGatewayCredential cred2 = new SchoolGatewayCredential(schoolId, digest2, "Backup");

        when(repository.findAllBySchoolId(schoolId)).thenReturn(List.of(cred1, cred2));

        List<SchoolGatewayCredential> list = repository.findAllBySchoolId(schoolId);

        assertThat(list).hasSize(2);
        assertThat(list)
                .extracting(SchoolGatewayCredential::getLabel)
                .containsExactly("Primary", "Backup");
    }
}
