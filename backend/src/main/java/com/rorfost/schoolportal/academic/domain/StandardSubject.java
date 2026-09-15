package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "standard_subjects")
public class StandardSubject extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID standardId;
  private UUID subjectId;
  private short sortOrder;

  protected StandardSubject() {}

  public StandardSubject(UUID schoolId, UUID standardId, UUID subjectId) {
    this(schoolId, standardId, subjectId, (short) 1);
  }

  public StandardSubject(UUID schoolId, UUID standardId, UUID subjectId, short sortOrder) {
    this.schoolId = schoolId;
    this.standardId = standardId;
    this.subjectId = subjectId;
    this.sortOrder = sortOrder;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getStandardId() {
    return standardId;
  }

  public UUID getSubjectId() {
    return subjectId;
  }

  public short getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(short sortOrder) {
    this.sortOrder = sortOrder;
  }
}
