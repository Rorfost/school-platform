package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "standards")
public class Standard extends AuditableUuidEntity {

  private UUID schoolId;
  private String code;
  private String displayName;
  private short sortOrder;
  private boolean isArchived;

  protected Standard() {}

  public Standard(UUID schoolId, String code, String displayName, short sortOrder) {
    this.schoolId = schoolId;
    this.code = code;
    this.displayName = displayName;
    this.sortOrder = sortOrder;
  }
}
