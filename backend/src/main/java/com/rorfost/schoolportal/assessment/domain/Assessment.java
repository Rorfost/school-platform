package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assessments")
public class Assessment extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private UUID standardId;
  private UUID assessmentTypeId;
  private String title;
  private String description;
  private LocalDate assessmentDate;

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

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }

  public UUID getStandardId() {
    return standardId;
  }

  public UUID getAssessmentTypeId() {
    return assessmentTypeId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getAssessmentDate() {
    return assessmentDate;
  }

  public AssessmentStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public void update(String title, String description, LocalDate assessmentDate) {
    this.title = title;
    this.description = description;
    this.assessmentDate = assessmentDate;
  }

  public void publish(Instant publishedAt) {
    this.status = AssessmentStatus.PUBLISHED;
    this.publishedAt = publishedAt;
    this.archivedAt = null;
  }

  public void archive(Instant archivedAt) {
    this.status = AssessmentStatus.ARCHIVED;
    this.archivedAt = archivedAt;
  }
}
