package com.rorfost.schoolportal.academic.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record StandardSubjectsUpdateRequest(@NotNull List<@NotNull UUID> subjectIds) {}
