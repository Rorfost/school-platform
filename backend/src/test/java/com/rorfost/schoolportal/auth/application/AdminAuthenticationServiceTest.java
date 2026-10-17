package com.rorfost.schoolportal.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

class AdminAuthenticationServiceTest {

    private AdminUserRepository adminUserRepository;
    private AuditLogService auditLogService;
    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private AdminAuthenticationService service;

    private UUID schoolAId;
    private UUID schoolBId;
    private AdminUser principalSchoolA;
    private AdminUser principalSchoolB;

    @BeforeEach
    void setUp() {
        adminUserRepository = Mockito.mock(AdminUserRepository.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        clock = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);

        service =
                new AdminAuthenticationService(
                        adminUserRepository, auditLogService, passwordEncoder, clock);

        schoolAId = UUID.randomUUID();
        schoolBId = UUID.randomUUID();

        principalSchoolA =
                new AdminUser(
                        schoolAId,
                        "principal@shared.edu",
                        "$2a$10$schoolAHashXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        principalSchoolB =
                new AdminUser(
                        schoolBId,
                        "principal@shared.edu",
                        "$2a$10$schoolBHashYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
    }

    @Test
    @DisplayName("Authenticates successfully for School A using tenant-scoped lookup")
    void authenticate_schoolA_success() {
        when(adminUserRepository.findBySchoolIdAndEmail(schoolAId, "principal@shared.edu"))
                .thenReturn(Optional.of(principalSchoolA));
        when(passwordEncoder.matches("PasswordSchoolA12!", principalSchoolA.getPasswordHash()))
                .thenReturn(true);

        PrincipalSession session =
                service.authenticate(schoolAId, "Principal@Shared.Edu", "PasswordSchoolA12!");

        assertThat(session.adminUserId()).isEqualTo(principalSchoolA.getId());
        assertThat(session.schoolId()).isEqualTo(schoolAId);
        assertThat(session.email()).isEqualTo("principal@shared.edu");

        verify(auditLogService)
                .record(
                        eq(schoolAId),
                        eq(principalSchoolA.getId()),
                        eq(AuditAction.LOGIN_SUCCESS),
                        eq("ADMIN_AUTH"),
                        eq(principalSchoolA.getId()),
                        any());
    }

    @Test
    @DisplayName(
            "Two principals with identical email authenticate ONLY against their respective school")
    void authenticate_identicalEmailDifferentSchools_isolated() {
        when(adminUserRepository.findBySchoolIdAndEmail(schoolAId, "principal@shared.edu"))
                .thenReturn(Optional.of(principalSchoolA));
        when(adminUserRepository.findBySchoolIdAndEmail(schoolBId, "principal@shared.edu"))
                .thenReturn(Optional.of(principalSchoolB));

        when(passwordEncoder.matches("PasswordSchoolA12!", principalSchoolA.getPasswordHash()))
                .thenReturn(true);
        when(passwordEncoder.matches("PasswordSchoolA12!", principalSchoolB.getPasswordHash()))
                .thenReturn(false);

        // Logging into School A with School A password succeeds
        PrincipalSession sessionA =
                service.authenticate(schoolAId, "principal@shared.edu", "PasswordSchoolA12!");
        assertThat(sessionA.schoolId()).isEqualTo(schoolAId);

        // Logging into School B with School A password fails
        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        schoolBId, "principal@shared.edu", "PasswordSchoolA12!"))
                .isInstanceOf(AuthenticationFailedException.class);

        // Failure audit is attributed to School B, never School A
        verify(auditLogService)
                .record(
                        eq(schoolBId),
                        eq(principalSchoolB.getId()),
                        eq(AuditAction.LOGIN_FAILED),
                        eq("ADMIN_AUTH"),
                        eq(principalSchoolB.getId()),
                        any());
    }

    @Test
    @DisplayName(
            "Wrong school portal cannot authenticate user of another school (not found in tenant)")
    void authenticate_wrongPortal_rejectedAsNotFoundInTenant() {
        // User exists only in School A
        when(adminUserRepository.findBySchoolIdAndEmail(schoolAId, "principal@shared.edu"))
                .thenReturn(Optional.of(principalSchoolA));
        when(adminUserRepository.findBySchoolIdAndEmail(schoolBId, "principal@shared.edu"))
                .thenReturn(Optional.empty());

        // Attacking or attempting login via School B portal fails immediately
        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        schoolBId, "principal@shared.edu", "PasswordSchoolA12!"))
                .isInstanceOf(AuthenticationFailedException.class);

        // Verify password check was never even attempted
        verify(passwordEncoder, never()).matches(any(), any());

        // Audit log failure is recorded for School B with null actor/target (no leak of School A
        // user ID)
        verify(auditLogService)
                .record(
                        eq(schoolBId),
                        eq(null),
                        eq(AuditAction.LOGIN_FAILED),
                        eq("ADMIN_AUTH"),
                        eq(null),
                        any());
    }

    @Test
    @DisplayName("Null schoolId throws AuthenticationFailedException without DB queries")
    void authenticate_nullSchoolId_failsImmediately() {
        assertThatThrownBy(
                        () ->
                                service.authenticate(
                                        null, "principal@shared.edu", "PasswordSchoolA12!"))
                .isInstanceOf(AuthenticationFailedException.class);

        verify(adminUserRepository, never()).findBySchoolIdAndEmail(any(), any());
    }
}
