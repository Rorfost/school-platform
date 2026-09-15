package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessments")
public class Assessment extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private UUID standardId;
  private UUID assessmentTypeId;
  private String title;

  @Enumerated(EnumType.STRING)
  private AssessmentStatus status = AssessmentStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected Assessment() {}

  public Assessment(
      UUID schoolId, UUID academicYearId, UUID standardId, UUID assessmentTypeId, String title) {
    this.schoolId = schoolId;
    this.academicYearId = academicYearId;
    this.standardId = standardId;
    this.assessmentTypeId = assessmentTypeId;
    this.title = title;
  }
}
