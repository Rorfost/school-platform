package com.rorfost.schoolportal.assessment.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AssessmentRequest(
    @NotNull UUID academicYearId,
    @NotNull UUID standardId,
    @NotNull UUID assessmentTypeId,
    @NotBlank @Size(max = 160) String title,
    String description,
    LocalDate assessmentDate,
    @NotEmpty List<@Valid AssessmentSubjectRequest> subjects) {
  public record AssessmentSubjectRequest(
      @NotNull UUID standardSubjectId,
      @NotNull BigDecimal maximumMarks,
      @NotNull BigDecimal passingMarks) {}
}
