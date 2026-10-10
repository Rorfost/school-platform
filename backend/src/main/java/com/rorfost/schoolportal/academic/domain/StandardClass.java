package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "standard_classes")
public class StandardClass extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID standardId;
  private String name;
  private short sortOrder;

  protected StandardClass() {}

  public StandardClass(UUID schoolId, UUID standardId, String name, short sortOrder) {
    this.schoolId = schoolId;
    this.standardId = standardId;
    this.name = name;
    this.sortOrder = sortOrder;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getStandardId() {
    return standardId;
  }

  public String getName() {
    return name;
  }

  public short getSortOrder() {
    return sortOrder;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSortOrder(short sortOrder) {
    this.sortOrder = sortOrder;
  }
}
