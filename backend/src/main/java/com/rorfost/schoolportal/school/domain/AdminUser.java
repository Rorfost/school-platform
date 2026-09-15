package com.rorfost.schoolportal.school.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_users")
public class AdminUser extends AuditableUuidEntity {

  private UUID schoolId;
  private String email;
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  private AdminRole role = AdminRole.PRINCIPAL;

  private boolean isActive = true;
  private boolean mustChangePassword = true;
  private Instant lastLoginAt;

  protected AdminUser() {}

  public AdminUser(UUID schoolId, String email, String passwordHash) {
    this.schoolId = schoolId;
    this.email = email;
    this.passwordHash = passwordHash;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public AdminRole getRole() {
    return role;
  }

  public boolean isActive() {
    return isActive;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void recordSuccessfulLogin(Instant occurredAt) {
    lastLoginAt = occurredAt;
  }

  public void changePassword(String newPasswordHash) {
    passwordHash = newPasswordHash;
    mustChangePassword = false;
  }
}
