package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.school.domain.PrincipalProfile;

public record PrincipalProfileResponse(
    String fullName,
    String biography,
    String qualification,
    String designation,
    String message,
    String portraitObjectKey,
    String signatureObjectKey,
    String signatureUrl,
    String email,
    String phone,
    boolean isPublic,
    boolean isContactPublic) {
  public static PrincipalProfileResponse admin(PrincipalProfile profile, String signatureUrl) {
    return new PrincipalProfileResponse(
        profile.getFullName(),
        profile.getBiography(),
        profile.getQualification(),
        profile.getDesignation(),
        profile.getMessage(),
        profile.getPortraitObjectKey(),
        profile.getSignatureObjectKey(),
        signatureUrl,
        profile.getEmail(),
        profile.getPhone(),
        profile.isPublic(),
        profile.isContactPublic());
  }

  public static PrincipalProfileResponse publicView(PrincipalProfile profile, String signatureUrl) {
    return new PrincipalProfileResponse(
        profile.getFullName(),
        profile.getBiography(),
        profile.getQualification(),
        profile.getDesignation(),
        profile.getMessage(),
        profile.getPortraitObjectKey(),
        profile.getSignatureObjectKey(),
        signatureUrl,
        profile.isContactPublic() ? profile.getEmail() : null,
        profile.isContactPublic() ? profile.getPhone() : null,
        true,
        profile.isContactPublic());
  }
}
