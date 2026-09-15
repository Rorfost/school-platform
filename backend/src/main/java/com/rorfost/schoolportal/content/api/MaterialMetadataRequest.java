package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record MaterialMetadataRequest(
    @NotBlank @Size(max = 160) String title,
    String description,
    @NotBlank @Size(max = 60) String materialType,
    UUID academicYearId,
    UUID standardSubjectId) {}
