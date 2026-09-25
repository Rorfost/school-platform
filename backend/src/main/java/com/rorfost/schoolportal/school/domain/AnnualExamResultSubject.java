package com.rorfost.schoolportal.school.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "annual_exam_result_subjects")
public class AnnualExamResultSubject {
  @Id private UUID id;

  @ManyToOne
  @JoinColumn(name = "exam_result_id")
  private AnnualExamResult examResult;

  @Column(name = "subject_name")
  private String subjectName;

  @Column(name = "maximum_marks")
  private Integer maximumMarks;

  @Column(name = "obtained_marks")
  private Integer obtainedMarks;

  private String grade;

  @Column(name = "sort_order")
  private Integer sortOrder;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  // Getters and Setters
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public AnnualExamResult getExamResult() { return examResult; }
  public void setExamResult(AnnualExamResult examResult) { this.examResult = examResult; }
  public String getSubjectName() { return subjectName; }
  public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
  public Integer getMaximumMarks() { return maximumMarks; }
  public void setMaximumMarks(Integer maximumMarks) { this.maximumMarks = maximumMarks; }
  public Integer getObtainedMarks() { return obtainedMarks; }
  public void setObtainedMarks(Integer obtainedMarks) { this.obtainedMarks = obtainedMarks; }
  public String getGrade() { return grade; }
  public void setGrade(String grade) { this.grade = grade; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
