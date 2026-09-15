package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private UUID standardId;
  private String fullName;
  private String rollNumber;
  private String resultPinHash;
  private boolean isArchived;

  protected Student() {}

  public Student(
      UUID schoolId,
      UUID academicYearId,
      UUID standardId,
      String fullName,
      String rollNumber,
      String resultPinHash) {
    this.schoolId = schoolId;
    this.academicYearId = academicYearId;
    this.standardId = standardId;
    this.fullName = fullName;
    this.rollNumber = rollNumber;
    this.resultPinHash = resultPinHash;
  }
}
