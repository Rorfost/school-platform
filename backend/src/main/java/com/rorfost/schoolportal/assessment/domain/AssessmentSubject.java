package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "assessment_subjects")
public class AssessmentSubject extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID assessmentId;
  private UUID standardId;
  private UUID standardSubjectId;
  private BigDecimal maximumMarks;
  private BigDecimal passingMarks;

  protected AssessmentSubject() {}

  public AssessmentSubject(
      UUID schoolId, UUID assessmentId, UUID standardId, UUID standardSubjectId) {
    this(schoolId, assessmentId, standardId, standardSubjectId, null, null);
  }

  public AssessmentSubject(
      UUID schoolId,
      UUID assessmentId,
      UUID standardId,
      UUID standardSubjectId,
      BigDecimal maximumMarks,
      BigDecimal passingMarks) {
    this.schoolId = schoolId;
    this.assessmentId = assessmentId;
    this.standardId = standardId;
    this.standardSubjectId = standardSubjectId;
    this.maximumMarks = maximumMarks;
    this.passingMarks = passingMarks;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getAssessmentId() {
    return assessmentId;
  }

  public UUID getStandardId() {
    return standardId;
  }

  public UUID getStandardSubjectId() {
    return standardSubjectId;
  }

  public BigDecimal getMaximumMarks() {
    return maximumMarks;
  }

  public BigDecimal getPassingMarks() {
    return passingMarks;
  }
}
