package com.rorfost.schoolportal.school.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "schools")
public class School extends AuditableUuidEntity {

  private String name;
  private String slug;
  private String shortName;
  private String schoolCode;
  private String address;
  private String city;
  private String state;
  private String postalCode;
  private String contactEmail;
  private String contactPhone;
  private String website;
  private String mapsUrl;
  private String about;
  private String establishedYear;
  private String medium;
  private String schoolType;
  private String logoObjectKey;
  private boolean isActive = true;

  protected School() {}

  public School(String name, String slug) {
    this.name = name;
    this.slug = slug;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getShortName() {
    return shortName;
  }

  public String getSchoolCode() {
    return schoolCode;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getState() {
    return state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public String getWebsite() {
    return website;
  }

  public String getMapsUrl() {
    return mapsUrl;
  }

  public String getAbout() {
    return about;
  }

  public String getEstablishedYear() {
    return establishedYear;
  }

  public String getMedium() {
    return medium;
  }

  public String getSchoolType() {
    return schoolType;
  }

  public String getLogoObjectKey() {
    return logoObjectKey;
  }

  public boolean isActive() {
    return isActive;
  }

  public void update(
      String name,
      String shortName,
      String schoolCode,
      String address,
      String city,
      String state,
      String postalCode,
      String contactEmail,
      String contactPhone,
      String website,
      String mapsUrl,
      String about,
      String establishedYear,
      String medium,
      String schoolType) {
    this.name = name;
    this.shortName = shortName;
    this.schoolCode = schoolCode;
    this.address = address;
    this.city = city;
    this.state = state;
    this.postalCode = postalCode;
    this.contactEmail = contactEmail;
    this.contactPhone = contactPhone;
    this.website = website;
    this.mapsUrl = mapsUrl;
    this.about = about;
    this.establishedYear = establishedYear;
    this.medium = medium;
    this.schoolType = schoolType;
  }

  public void changeLogo(String logoObjectKey) {
    this.logoObjectKey = logoObjectKey;
  }
}
