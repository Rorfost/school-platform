package com.rorfost.schoolportal.school.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "principal_profiles")
public class PrincipalProfile extends AuditableUuidEntity {

  private UUID schoolId;
  private String fullName;
  private String biography;
  private String qualification;
  private String designation;
  private String message;
  private String portraitObjectKey;
  private String email;
  private String phone;
  private boolean isPublic = true;
  private boolean isContactPublic;

  protected PrincipalProfile() {}

  public PrincipalProfile(UUID schoolId, String fullName) {
    this.schoolId = schoolId;
    this.fullName = fullName;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getFullName() {
    return fullName;
  }

  public String getBiography() {
    return biography;
  }

  public String getQualification() {
    return qualification;
  }

  public String getDesignation() {
    return designation;
  }

  public String getMessage() {
    return message;
  }

  public String getPortraitObjectKey() {
    return portraitObjectKey;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public boolean isContactPublic() {
    return isContactPublic;
  }

  public void update(
      String fullName,
      String biography,
      String qualification,
      String designation,
      String message,
      String portraitObjectKey,
      String email,
      String phone,
      boolean isPublic,
      boolean isContactPublic) {
    this.fullName = fullName;
    this.biography = biography;
    this.qualification = qualification;
    this.designation = designation;
    this.message = message;
    this.portraitObjectKey = portraitObjectKey;
    this.email = email;
    this.phone = phone;
    this.isPublic = isPublic;
    this.isContactPublic = isContactPublic;
  }
}
