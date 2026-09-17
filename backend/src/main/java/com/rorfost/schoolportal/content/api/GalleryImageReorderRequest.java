package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record GalleryImageReorderRequest(@NotEmpty List<@NotNull UUID> imageIds) {}
