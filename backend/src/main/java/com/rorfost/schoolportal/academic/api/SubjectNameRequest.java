package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectNameRequest(@NotBlank @Size(max = 120) String name) {}
