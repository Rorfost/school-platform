package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AcademicYearRequest(
    @NotBlank @Size(max = 40) String name,
    @NotNull LocalDate startsOn,
    @NotNull LocalDate endsOn,
    @NotNull Boolean current) {}
