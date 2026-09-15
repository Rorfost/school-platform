package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
    @NotBlank @Size(max = 30) String code,
    @NotBlank @Size(max = 120) String name,
    @NotNull @Positive Short sortOrder,
    @NotNull Boolean archived) {}
