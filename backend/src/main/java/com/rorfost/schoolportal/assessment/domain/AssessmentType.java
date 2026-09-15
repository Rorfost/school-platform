package com.rorfost.schoolportal.assessment.domain;

import com.rorfost.schoolportal.common.persistence.UuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "assessment_types")
public class AssessmentType extends UuidEntity {

  private String code;
  private String displayName;
  private short sortOrder;
  private boolean isActive;

  protected AssessmentType() {}

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public short getSortOrder() {
    return sortOrder;
  }

  public boolean isActive() {
    return isActive;
  }
}
