package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record StandardRequest(
    @NotBlank @Size(max = 20) String code,
    @NotBlank @Size(max = 80) String displayName,
    @NotNull @Positive Short sortOrder,
    @NotNull Boolean archived) {}
