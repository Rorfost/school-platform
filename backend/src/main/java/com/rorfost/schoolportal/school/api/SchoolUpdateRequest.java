package com.rorfost.schoolportal.school.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolUpdateRequest(
    @NotBlank @Size(max = 160) String name,
    @Size(max = 80) String shortName,
    @Size(max = 40) String schoolCode,
    String address,
    @Size(max = 100) String city,
    @Size(max = 100) String state,
    @Size(max = 20) String postalCode,
    @Email @Size(max = 254) String email,
    @Size(max = 40) String phone,
    @Size(max = 255) String website,
    @Size(max = 2048) String mapsUrl,
    String about,
    Short establishedYear,
    @Size(max = 80) String medium,
    @Size(max = 80) String schoolType) {}
