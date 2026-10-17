package com.rorfost.schoolportal.tenant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.repository.SchoolGatewayCredentialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

class TenantResolverTest {

    private SchoolGatewayCredentialRepository credentialRepository;
    private SchoolRepository schoolRepository;
    private TenantResolver tenantResolver;

    @BeforeEach
    void setUp() {
        credentialRepository = mock(SchoolGatewayCredentialRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        tenantResolver = new TenantResolver(credentialRepository, schoolRepository);
    }

    @Test
    void resolveTenant_shouldReturnTenantContextForValidActiveTokenAndSchool() {
        String rawToken = "cf-worker-valid-token-32bytes-entropy";
        byte[] digest = SchoolGatewayCredential.computeDigest(rawToken);

        UUID schoolId = UUID.randomUUID();
        SchoolGatewayCredential credential =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Primary");
        School school = mock(School.class);
        when(school.getId()).thenReturn(schoolId);
        when(school.getSlug()).thenReturn("dhadhana-primary");
        when(school.isActive()).thenReturn(true);

        when(credentialRepository.findActiveByTokenDigest(eq(digest), any(Instant.class)))
                .thenReturn(Optional.of(credential));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        Optional<TenantContext> result = tenantResolver.resolveTenant(rawToken);

        assertThat(result).isPresent();
        assertThat(result.get().schoolId()).isEqualTo(schoolId);
        assertThat(result.get().slug()).isEqualTo("dhadhana-primary");
    }

    @Test
    void resolveTenant_shouldReturnEmptyForBlankOrNullToken() {
        assertThat(tenantResolver.resolveTenant(null)).isEmpty();
        assertThat(tenantResolver.resolveTenant("")).isEmpty();
        assertThat(tenantResolver.resolveTenant("   ")).isEmpty();
    }

    @Test
    void resolveTenant_shouldReturnEmptyWhenCredentialNotFound() {
        String rawToken = "unknown-token";
        byte[] digest = SchoolGatewayCredential.computeDigest(rawToken);

        when(credentialRepository.findActiveByTokenDigest(eq(digest), any(Instant.class)))
                .thenReturn(Optional.empty());

        Optional<TenantContext> result = tenantResolver.resolveTenant(rawToken);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveTenant_shouldReturnEmptyWhenMappedSchoolNotFound() {
        String rawToken = "valid-token-missing-school";
        byte[] digest = SchoolGatewayCredential.computeDigest(rawToken);

        UUID schoolId = UUID.randomUUID();
        SchoolGatewayCredential credential =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Primary");

        when(credentialRepository.findActiveByTokenDigest(eq(digest), any(Instant.class)))
                .thenReturn(Optional.of(credential));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.empty());

        Optional<TenantContext> result = tenantResolver.resolveTenant(rawToken);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveTenant_shouldReturnEmptyWhenSchoolIsInactive() {
        String rawToken = "valid-token-inactive-school";
        byte[] digest = SchoolGatewayCredential.computeDigest(rawToken);

        UUID schoolId = UUID.randomUUID();
        SchoolGatewayCredential credential =
                new SchoolGatewayCredential(schoolId, digest, "Worker-Primary");
        School inactiveSchool = mock(School.class);
        when(inactiveSchool.isActive()).thenReturn(false);

        when(credentialRepository.findActiveByTokenDigest(eq(digest), any(Instant.class)))
                .thenReturn(Optional.of(credential));
        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(inactiveSchool));

        Optional<TenantContext> result = tenantResolver.resolveTenant(rawToken);

        assertThat(result).isEmpty();
    }
}
