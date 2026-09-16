package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StudentRequest(
    @NotNull UUID academicYearId,
    @NotNull UUID standardId,
    @NotBlank @Size(max = 160) String fullName,
    @NotBlank @Size(max = 32) String rollNumber,
    @Size(min = 4, max = 10) String resultPin,
    boolean isArchived) {}
