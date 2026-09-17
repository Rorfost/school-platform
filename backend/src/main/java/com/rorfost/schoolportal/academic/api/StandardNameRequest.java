package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Principal-facing creation keeps internal codes and display ordering server managed. */
public record StandardNameRequest(@NotBlank @Size(max = 80) String displayName) {}
