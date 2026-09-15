package com.rorfost.schoolportal.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank String currentPassword, @NotBlank @Size(min = 12, max = 72) String newPassword) {}
