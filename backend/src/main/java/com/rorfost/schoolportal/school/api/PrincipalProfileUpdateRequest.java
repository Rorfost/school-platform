package com.rorfost.schoolportal.school.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrincipalProfileUpdateRequest(
    @NotBlank @Size(max = 160) String fullName,
    String biography,
    @Size(max = 160) String qualification,
    @Size(max = 120) String designation,
    String message,
    @Size(max = 512) String portraitObjectKey,
    @Email @Size(max = 254) String email,
    @Size(max = 40) String phone,
    @NotNull Boolean isPublic,
    @NotNull Boolean isContactPublic) {}
