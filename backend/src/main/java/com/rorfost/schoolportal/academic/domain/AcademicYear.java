package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_years")
public class AcademicYear extends AuditableUuidEntity {

  private UUID schoolId;
  private String name;
  private LocalDate startsOn;
  private LocalDate endsOn;

  @Enumerated(EnumType.STRING)
  private AcademicYearStatus status = AcademicYearStatus.CURRENT;

  private Instant archivedAt;

  protected AcademicYear() {}

  public AcademicYear(
      UUID schoolId,
      String name,
      LocalDate startsOn,
      LocalDate endsOn,
      AcademicYearStatus status,
      Instant archivedAt) {
    this.schoolId = schoolId;
    this.name = name;
    this.startsOn = startsOn;
    this.endsOn = endsOn;
    this.status = status;
    this.archivedAt = archivedAt;
  }
}
