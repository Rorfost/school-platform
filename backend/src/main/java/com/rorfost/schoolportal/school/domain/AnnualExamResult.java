package com.rorfost.schoolportal.school.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "annual_exam_results")
public class AnnualExamResult {
  @Id private UUID id;

  @Column(name = "school_id")
  private UUID schoolId;

  @Column(name = "result_type")
  private String resultType;

  @Column(name = "student_name")
  private String studentName;

  private String standard;

  @Column(name = "roll_number")
  private Integer rollNumber;

  @Column(name = "general_register_number")
  private String generalRegisterNumber;

  @Column(name = "birth_date")
  private String birthDate;

  @Column(name = "total_working_days")
  private Integer totalWorkingDays;

  @Column(name = "attended_days")
  private Integer attendedDays;

  @Column(name = "total_marks")
  private Integer totalMarks;

  @Column(name = "obtained_marks")
  private Integer obtainedMarks;

  private BigDecimal percentage;

  @Column(name = "overall_grade")
  private String overallGrade;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AnnualExamResultSubject> subjects = new ArrayList<>();

  // Getters and Setters

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public void setSchoolId(UUID schoolId) {
    this.schoolId = schoolId;
  }

  public String getResultType() {
    return resultType;
  }

  public void setResultType(String resultType) {
    this.resultType = resultType;
  }

  public String getStudentName() {
    return studentName;
  }

  public void setStudentName(String studentName) {
    this.studentName = studentName;
  }

  public String getStandard() {
    return standard;
  }

  public void setStandard(String standard) {
    this.standard = standard;
  }

  public Integer getRollNumber() {
    return rollNumber;
  }

  public void setRollNumber(Integer rollNumber) {
    this.rollNumber = rollNumber;
  }

  public String getGeneralRegisterNumber() {
    return generalRegisterNumber;
  }

  public void setGeneralRegisterNumber(String generalRegisterNumber) {
    this.generalRegisterNumber = generalRegisterNumber;
  }

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public Integer getTotalWorkingDays() {
    return totalWorkingDays;
  }

  public void setTotalWorkingDays(Integer totalWorkingDays) {
    this.totalWorkingDays = totalWorkingDays;
  }

  public Integer getAttendedDays() {
    return attendedDays;
  }

  public void setAttendedDays(Integer attendedDays) {
    this.attendedDays = attendedDays;
  }

  public Integer getTotalMarks() {
    return totalMarks;
  }

  public void setTotalMarks(Integer totalMarks) {
    this.totalMarks = totalMarks;
  }

  public Integer getObtainedMarks() {
    return obtainedMarks;
  }

  public void setObtainedMarks(Integer obtainedMarks) {
    this.obtainedMarks = obtainedMarks;
  }

  public BigDecimal getPercentage() {
    return percentage;
  }

  public void setPercentage(BigDecimal percentage) {
    this.percentage = percentage;
  }

  public String getOverallGrade() {
    return overallGrade;
  }

  public void setOverallGrade(String overallGrade) {
    this.overallGrade = overallGrade;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<AnnualExamResultSubject> getSubjects() {
    return subjects;
  }

  public void setSubjects(List<AnnualExamResultSubject> subjects) {
    this.subjects = subjects;
  }

  public void addSubject(AnnualExamResultSubject subject) {
    subjects.add(subject);
    subject.setExamResult(this);
  }
}
