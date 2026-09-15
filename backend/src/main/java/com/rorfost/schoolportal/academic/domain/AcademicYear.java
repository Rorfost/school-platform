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

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getName() {
    return name;
  }

  public LocalDate getStartsOn() {
    return startsOn;
  }

  public LocalDate getEndsOn() {
    return endsOn;
  }

  public AcademicYearStatus getStatus() {
    return status;
  }

  public Instant getArchivedAt() {
    return archivedAt;
  }

  public boolean isArchived() {
    return status == AcademicYearStatus.ARCHIVED;
  }

  public void update(String name, LocalDate startsOn, LocalDate endsOn) {
    this.name = name;
    this.startsOn = startsOn;
    this.endsOn = endsOn;
  }

  public void archive(Instant archivedAt) {
    this.status = AcademicYearStatus.ARCHIVED;
    this.archivedAt = archivedAt;
  }

  public void markCurrent() {
    this.status = AcademicYearStatus.CURRENT;
    this.archivedAt = null;
  }
}
