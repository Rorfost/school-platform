package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "marks")
public class Mark extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private UUID standardId;
  private UUID assessmentId;
  private UUID studentId;
  private UUID assessmentSubjectId;

  @Column(precision = 7, scale = 2)
  private BigDecimal score;

  protected Mark() {}

  public Mark(
      UUID schoolId,
      UUID academicYearId,
      UUID standardId,
      UUID assessmentId,
      UUID studentId,
      UUID assessmentSubjectId,
      BigDecimal score) {
    this.schoolId = schoolId;
    this.academicYearId = academicYearId;
    this.standardId = standardId;
    this.assessmentId = assessmentId;
    this.studentId = studentId;
    this.assessmentSubjectId = assessmentSubjectId;
    this.score = score;
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

  public UUID getAssessmentId() {
    return assessmentId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getAssessmentSubjectId() {
    return assessmentSubjectId;
  }

  public BigDecimal getScore() {
    return score;
  }
}
