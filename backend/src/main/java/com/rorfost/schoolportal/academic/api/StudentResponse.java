package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.Student;
import java.time.Instant;
import java.util.UUID;

public record StudentResponse(
    UUID id,
    UUID schoolId,
    UUID academicYearId,
    UUID standardId,
    String fullName,
    String rollNumber,
    boolean isArchived,
    Instant createdAt,
    Instant updatedAt) {

  public static StudentResponse from(Student student) {
    return new StudentResponse(
        student.getId(),
        student.getSchoolId(),
        student.getAcademicYearId(),
        student.getStandardId(),
        student.getFullName(),
        student.getRollNumber(),
        student.isArchived(),
        student.getCreatedAt(),
        student.getUpdatedAt());
  }
}
