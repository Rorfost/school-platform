package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.domain.Assessment;
import com.rorfost.schoolportal.assessment.domain.AssessmentStatus;
import com.rorfost.schoolportal.assessment.domain.AssessmentSubject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssessmentResponse(
    UUID id,
    UUID academicYearId,
    UUID standardId,
    UUID assessmentTypeId,
    String title,
    String description,
    LocalDate assessmentDate,
    AssessmentStatus status,
    List<Subject> subjects) {
  public record Subject(UUID standardSubjectId, BigDecimal maximumMarks, BigDecimal passingMarks) {}

  public static AssessmentResponse from(Assessment assessment, List<AssessmentSubject> subjects) {
    return new AssessmentResponse(
        assessment.getId(),
        assessment.getAcademicYearId(),
        assessment.getStandardId(),
        assessment.getAssessmentTypeId(),
        assessment.getTitle(),
        assessment.getDescription(),
        assessment.getAssessmentDate(),
        assessment.getStatus(),
        subjects.stream()
            .map(
                value ->
                    new Subject(
                        value.getStandardSubjectId(),
                        value.getMaximumMarks(),
                        value.getPassingMarks()))
            .toList());
  }
}
