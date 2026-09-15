package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearResponse(
    UUID id, String name, LocalDate startsOn, LocalDate endsOn, AcademicYearStatus status) {
  public static AcademicYearResponse from(AcademicYear value) {
    return new AcademicYearResponse(
        value.getId(), value.getName(), value.getStartsOn(), value.getEndsOn(), value.getStatus());
  }
}
