package com.rorfost.schoolportal.content.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record NoticeRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank String body,
    boolean pinned,
    Instant expiresAt) {}
