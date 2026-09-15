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
  private Instant lastLoginAt;

  protected AdminUser() {}
}
