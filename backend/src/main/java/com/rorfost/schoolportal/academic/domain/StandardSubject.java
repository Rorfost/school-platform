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

  protected StandardSubject() {}

  public StandardSubject(UUID schoolId, UUID standardId, UUID subjectId) {
    this.schoolId = schoolId;
    this.standardId = standardId;
    this.subjectId = subjectId;
  }
}
