package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record StandardSubjectRequest(
    @NotNull UUID standardId,
    @NotNull UUID subjectId,
    @Positive short sortOrder,
    @Positive int maximumMarks) {}
