package com.rorfost.schoolportal.school.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "schools")
public class School extends AuditableUuidEntity {

  private String name;
  private String slug;
  private String address;
  private String contactEmail;
  private String contactPhone;
  private String logoObjectKey;
  private boolean isActive = true;

  protected School() {}

  public School(String name, String slug) {
    this.name = name;
    this.slug = slug;
  }
}
