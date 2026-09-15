package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject extends AuditableUuidEntity {

  private UUID schoolId;
  private String code;
  private String name;
  private short sortOrder;
  private boolean isArchived;

  protected Subject() {}

  public Subject(UUID schoolId, String code, String name, short sortOrder) {
    this.schoolId = schoolId;
    this.code = code;
    this.name = name;
    this.sortOrder = sortOrder;
  }
}
