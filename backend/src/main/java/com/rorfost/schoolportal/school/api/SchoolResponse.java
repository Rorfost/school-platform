package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.school.domain.School;
import java.util.UUID;

public record SchoolResponse(
    UUID id,
    String name,
    String shortName,
    String schoolCode,
    String address,
    String city,
    String state,
    String postalCode,
    String email,
    String phone,
    String website,
    String mapsUrl,
    String about,
    Short establishedYear,
    String medium,
    String schoolType,
    String logoUrl) {
  public static SchoolResponse from(School school, String logoUrl) {
    return new SchoolResponse(
        school.getId(),
        school.getName(),
        school.getShortName(),
        school.getSchoolCode(),
        school.getAddress(),
        school.getCity(),
        school.getState(),
        school.getPostalCode(),
        school.getContactEmail(),
        school.getContactPhone(),
        school.getWebsite(),
        school.getMapsUrl(),
        school.getAbout(),
        school.getEstablishedYear(),
        school.getMedium(),
        school.getSchoolType(),
        logoUrl);
  }
}
