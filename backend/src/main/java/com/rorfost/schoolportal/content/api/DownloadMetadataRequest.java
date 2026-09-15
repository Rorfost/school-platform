package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DownloadMetadataRequest(
    @NotBlank @Size(max = 160) String title,
    String description,
    @Size(max = 60) String category,
    UUID academicYearId) {}
