package com.rorfost.schoolportal.school.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "principal_profiles")
public class PrincipalProfile extends AuditableUuidEntity {

  private UUID schoolId;
  private String fullName;
  private String biography;
  private String portraitObjectKey;

  protected PrincipalProfile() {}
}
