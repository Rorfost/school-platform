package com.rorfost.schoolportal.tenant.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.domain.SchoolDomain;
import com.rorfost.schoolportal.tenant.domain.SchoolGatewayCredential;
import com.rorfost.schoolportal.tenant.repository.SchoolDomainRepository;
import com.rorfost.schoolportal.tenant.repository.SchoolGatewayCredentialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

class TenantProvisioningServiceTest {

    private final SchoolRepository schoolRepository = Mockito.mock(SchoolRepository.class);
    private final SchoolDomainRepository schoolDomainRepository =
            Mockito.mock(SchoolDomainRepository.class);
    private final SchoolGatewayCredentialRepository credentialRepository =
            Mockito.mock(SchoolGatewayCredentialRepository.class);
    private final AdminUserRepository adminUserRepository = Mockito.mock(AdminUserRepository.class);
    private final PrincipalProfileRepository principalProfileRepository =
            Mockito.mock(PrincipalProfileRepository.class);
    private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    private final AuditLogService auditLogService = Mockito.mock(AuditLogService.class);

    private TenantProvisioningService service;

    @BeforeEach
    void setUp() {
        service =
                new TenantProvisioningService(
                        schoolRepository,
                        schoolDomainRepository,
                        credentialRepository,
                        adminUserRepository,
                        principalProfileRepository,
                        passwordEncoder,
                        auditLogService);

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        when(schoolRepository.save(any(School.class)))
                .thenAnswer(
                        inv -> {
                            School s = inv.getArgument(0);
                            if (s.getId() == null) {
                                ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
                            }
                            return s;
                        });
        when(schoolDomainRepository.save(any(SchoolDomain.class)))
                .thenAnswer(
                        inv -> {
                            SchoolDomain d = inv.getArgument(0);
                            if (d.getId() == null) {
                                ReflectionTestUtils.setField(d, "id", UUID.randomUUID());
                            }
                            return d;
                        });
        when(credentialRepository.save(any(SchoolGatewayCredential.class)))
                .thenAnswer(
                        inv -> {
                            SchoolGatewayCredential c = inv.getArgument(0);
                            if (c.getId() == null) {
                                ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
                            }
                            return c;
                        });
    }

    @Test
    void provisionSchoolCreatesAllRequiredEntitiesAndDigest() {
        String slug = "school-b";
        String name = "School B";
        String hostname = "school-b.workers.dev";
        String email = "principal@school-b.org";
        String pass = "SecurePass123!";

        when(schoolRepository.findBySlug(slug)).thenReturn(Optional.empty());
        when(schoolDomainRepository.findByHostname(hostname)).thenReturn(Optional.empty());

        ProvisioningResult result = service.provisionSchool(slug, name, hostname, email, pass);

        assertThat(result.slug()).isEqualTo(slug);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.hostname()).isEqualTo(hostname);
        assertThat(result.rawGatewayToken()).isNotBlank();
        assertThat(result.isNewSchool()).isTrue();

        // Verify school saved
        verify(schoolRepository).save(any(School.class));
        // Verify domain saved
        verify(schoolDomainRepository).save(any(SchoolDomain.class));
        // Verify principal profile saved
        verify(principalProfileRepository).save(any(PrincipalProfile.class));
        // Verify admin saved with one-time change required
        ArgumentCaptor<AdminUser> adminCaptor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(adminCaptor.capture());
        assertThat(adminCaptor.getValue().isMustChangePassword()).isTrue();
        assertThat(adminCaptor.getValue().getEmail()).isEqualTo(email);

        // Verify gateway credential digest saved
        ArgumentCaptor<SchoolGatewayCredential> credCaptor =
                ArgumentCaptor.forClass(SchoolGatewayCredential.class);
        verify(credentialRepository).save(credCaptor.capture());
        SchoolGatewayCredential cred = credCaptor.getValue();
        assertThat(cred.getTokenDigest()).isNotNull();
        assertThat(cred.isActive()).isTrue();

        // Verify audit logged
        verify(auditLogService)
                .record(
                        eq(result.schoolId()),
                        any(),
                        eq(AuditAction.TENANT_PROVISIONED),
                        any(),
                        any(),
                        any());
    }

    @Test
    void provisionSchoolRequiresPasswordWhenCreatingNewPrincipal() {
        String slug = "school-b";
        String name = "School B";
        String hostname = "school-b.workers.dev";
        String email = "principal@school-b.org";

        when(schoolRepository.findBySlug(slug)).thenReturn(Optional.empty());
        when(schoolDomainRepository.findByHostname(hostname)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.provisionSchool(slug, name, hostname, email, null))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("initial_password_required_for_principal"));

        assertThatThrownBy(() -> service.provisionSchool(slug, name, hostname, email, "   "))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("initial_password_required_for_principal"));
    }

    @Test
    void provisionSchoolIsIdempotentForSameSchoolAndDoesNotDuplicateActiveCredentials() {
        String slug = "school-b";
        String name = "School B";
        String hostname = "school-b.workers.dev";
        String email = "principal@school-b.org";

        UUID schoolId = UUID.randomUUID();
        School existingSchool = new School(name, slug);
        ReflectionTestUtils.setField(existingSchool, "id", schoolId);

        when(schoolRepository.findBySlug(slug)).thenReturn(Optional.of(existingSchool));
        SchoolDomain existingDomain = new SchoolDomain(schoolId, hostname, true);
        when(schoolDomainRepository.findByHostname(hostname))
                .thenReturn(Optional.of(existingDomain));
        when(adminUserRepository.findBySchoolIdAndEmail(schoolId, email))
                .thenReturn(Optional.of(new AdminUser(schoolId, email, "existingHash")));

        UUID existingCredId = UUID.randomUUID();
        SchoolGatewayCredential activeCred =
                new SchoolGatewayCredential(
                        schoolId, SchoolGatewayCredential.computeDigest("dummy"), "active-token");
        ReflectionTestUtils.setField(activeCred, "id", existingCredId);

        when(credentialRepository.findActiveBySchoolId(eq(schoolId), any(Instant.class)))
                .thenReturn(java.util.List.of(activeCred));

        ProvisioningResult result = service.provisionSchool(slug, name, hostname, email, null);

        assertThat(result.isNewSchool()).isFalse();
        assertThat(result.schoolId()).isEqualTo(schoolId);
        // Verify raw token is null because existing active credential was preserved
        assertThat(result.rawGatewayToken()).isNull();
        assertThat(result.credentialId()).isEqualTo(existingCredId);
        // Verify no duplicate admin user was created
        verify(adminUserRepository, Mockito.never()).save(any());
        // Verify no new credential was saved
        verify(credentialRepository, Mockito.never()).save(any());
    }

    @Test
    void provisionSchoolRejectsHostnameAlreadyBoundToDifferentSchool() {
        String slug = "school-c";
        String hostname = "school-b.workers.dev"; // already belongs to School B

        UUID schoolBId = UUID.randomUUID();
        SchoolDomain existingDomain = new SchoolDomain(schoolBId, hostname, true);
        when(schoolDomainRepository.findByHostname(hostname))
                .thenReturn(Optional.of(existingDomain));
        when(schoolRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.provisionSchool(
                                        slug, "School C", hostname, "p@school-c.org", null))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("hostname_already_bound_to_different_school"))
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getStatus())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rotateGatewayCredentialGeneratesNewTokenAndSetsGracePeriod() {
        UUID schoolId = UUID.randomUUID();
        UUID oldCredId = UUID.randomUUID();

        School school = new School("School B", "school-b");
        ReflectionTestUtils.setField(school, "id", schoolId);

        byte[] oldDigest = SchoolGatewayCredential.computeDigest("old-raw-token");
        SchoolGatewayCredential oldCred =
                new SchoolGatewayCredential(schoolId, oldDigest, "label-old");
        ReflectionTestUtils.setField(oldCred, "id", oldCredId);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));
        when(credentialRepository.findById(oldCredId)).thenReturn(Optional.of(oldCred));

        TokenRotationResult result =
                service.rotateGatewayCredential(schoolId, oldCredId, Duration.ofHours(48));

        assertThat(result.oldCredentialId()).isEqualTo(oldCredId);
        assertThat(result.rawNewGatewayToken()).isNotBlank();
        assertThat(result.oldExpiresAt()).isAfter(Instant.now());
        assertThat(oldCred.getRotatedAt()).isNotNull();

        verify(auditLogService)
                .record(
                        eq(schoolId),
                        any(),
                        eq(AuditAction.GATEWAY_TOKEN_ROTATED),
                        any(),
                        any(),
                        any());
    }

    @Test
    void revokeGatewayCredentialMarksRevokedImmediately() {
        UUID schoolId = UUID.randomUUID();
        UUID credId = UUID.randomUUID();

        byte[] digest = SchoolGatewayCredential.computeDigest("raw-token");
        SchoolGatewayCredential cred = new SchoolGatewayCredential(schoolId, digest, "label-1");
        ReflectionTestUtils.setField(cred, "id", credId);

        when(credentialRepository.findById(credId)).thenReturn(Optional.of(cred));

        service.revokeGatewayCredential(schoolId, credId);

        assertThat(cred.isActive()).isFalse();
        assertThat(cred.getRevokedAt()).isNotNull();

        verify(auditLogService)
                .record(
                        eq(schoolId),
                        any(),
                        eq(AuditAction.GATEWAY_TOKEN_REVOKED),
                        any(),
                        eq(credId),
                        any());
    }

    @Test
    void deactivateAndActivateSchoolTogglesStatus() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("School B", "school-b");
        ReflectionTestUtils.setField(school, "id", schoolId);

        when(schoolRepository.findById(schoolId)).thenReturn(Optional.of(school));

        service.deactivateSchool(schoolId);
        assertThat(school.isActive()).isFalse();
        verify(auditLogService)
                .record(
                        eq(schoolId),
                        any(),
                        eq(AuditAction.TENANT_DEACTIVATED),
                        any(),
                        eq(schoolId),
                        any());

        service.activateSchool(schoolId);
        assertThat(school.isActive()).isTrue();
        verify(auditLogService)
                .record(
                        eq(schoolId),
                        any(),
                        eq(AuditAction.TENANT_ACTIVATED),
                        any(),
                        eq(schoolId),
                        any());
    }
}
