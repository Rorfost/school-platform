package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GalleryImageMetadataRequest(
    @NotBlank @Size(max = 255) String altText, String caption) {}
