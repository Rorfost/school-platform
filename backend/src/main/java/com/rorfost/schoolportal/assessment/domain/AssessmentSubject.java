package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "assessment_subjects")
public class AssessmentSubject extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID assessmentId;
  private UUID standardId;
  private UUID standardSubjectId;

  protected AssessmentSubject() {}

  public AssessmentSubject(
      UUID schoolId, UUID assessmentId, UUID standardId, UUID standardSubjectId) {
    this.schoolId = schoolId;
    this.assessmentId = assessmentId;
    this.standardId = standardId;
    this.standardSubjectId = standardSubjectId;
  }
}
