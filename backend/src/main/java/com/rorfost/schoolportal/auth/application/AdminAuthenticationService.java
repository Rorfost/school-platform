package com.rorfost.schoolportal.auth.application;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthenticationService {

  private static final String AUTH_TARGET = "ADMIN_AUTH";

  private final AdminUserRepository adminUserRepository;
  private final SchoolRepository schoolRepository;
  private final AuditLogService auditLogService;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Autowired
  public AdminAuthenticationService(
      AdminUserRepository adminUserRepository,
      SchoolRepository schoolRepository,
      AuditLogService auditLogService,
      PasswordEncoder passwordEncoder) {
    this(
        adminUserRepository, schoolRepository, auditLogService, passwordEncoder, Clock.systemUTC());
  }

  AdminAuthenticationService(
      AdminUserRepository adminUserRepository,
      SchoolRepository schoolRepository,
      AuditLogService auditLogService,
      PasswordEncoder passwordEncoder,
      Clock clock) {
    this.adminUserRepository = adminUserRepository;
    this.schoolRepository = schoolRepository;
    this.auditLogService = auditLogService;
    this.passwordEncoder = passwordEncoder;
    this.clock = clock;
  }

  @Transactional
  public PrincipalSession authenticate(String email, String password) {
    List<AdminUser> matches = adminUserRepository.findAllByEmail(normalizeEmail(email));
    if (matches.size() != 1) {
      recordFailedLogin(matches.isEmpty() ? null : matches.getFirst());
      throw new AuthenticationFailedException();
    }

    AdminUser user = matches.getFirst();
    if (!user.isActive() || !passwordEncoder.matches(password, user.getPasswordHash())) {
      recordFailedLogin(user);
      throw new AuthenticationFailedException();
    }

    user.recordSuccessfulLogin(Instant.now(clock));
    auditLogService.record(
        user.getSchoolId(),
        user.getId(),
        AuditAction.LOGIN_SUCCESS,
        AUTH_TARGET,
        user.getId(),
        MDC.get("requestId"));
    return toSession(user);
  }

  @Transactional
  public PrincipalSession changePassword(
      PrincipalSession principal, String currentPassword, String newPassword) {
    AdminUser user =
        adminUserRepository
            .findById(principal.adminUserId())
            .filter(AdminUser::isActive)
            .orElseThrow(AuthenticationFailedException::new);
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())
        || passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new PasswordChangeException();
    }

    user.changePassword(passwordEncoder.encode(newPassword));
    auditLogService.record(
        user.getSchoolId(),
        user.getId(),
        AuditAction.PASSWORD_CHANGED,
        "ADMIN_USER",
        user.getId(),
        MDC.get("requestId"));
    return toSession(user);
  }

  private void recordFailedLogin(AdminUser user) {
    if (user != null) {
      auditLogService.record(
          user.getSchoolId(),
          user.getId(),
          AuditAction.LOGIN_FAILED,
          AUTH_TARGET,
          user.getId(),
          MDC.get("requestId"));
      return;
    }
    schoolRepository
        .findFirstByOrderByCreatedAtAsc()
        .ifPresent(
            school ->
                auditLogService.record(
                    school.getId(),
                    null,
                    AuditAction.LOGIN_FAILED,
                    AUTH_TARGET,
                    null,
                    MDC.get("requestId")));
  }

  private PrincipalSession toSession(AdminUser user) {
    return new PrincipalSession(
        user.getId(),
        user.getSchoolId(),
        user.getEmail(),
        user.getRole(),
        user.isMustChangePassword());
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
