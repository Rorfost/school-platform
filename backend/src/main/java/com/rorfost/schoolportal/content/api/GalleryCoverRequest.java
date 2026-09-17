package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GalleryCoverRequest(@NotNull UUID imageId) {}
