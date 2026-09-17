package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GalleryAlbumUpdateRequest(
    @NotBlank @Size(max = 160) String title, String description) {}
