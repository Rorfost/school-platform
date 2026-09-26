package com.rorfost.schoolportal.academic.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "standards")
public class Standard extends AuditableUuidEntity {

  private UUID schoolId;
  private String code;
  private String displayName;
  private short sortOrder;
  private boolean isArchived;
  private String classTeacherName;
  private String classTeacherSignatureObjectKey;

  protected Standard() {}

  public Standard(UUID schoolId, String code, String displayName, short sortOrder) {
    this.schoolId = schoolId;
    this.code = code;
    this.displayName = displayName;
    this.sortOrder = sortOrder;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public short getSortOrder() {
    return sortOrder;
  }

  public boolean isArchived() {
    return isArchived;
  }

  public String getClassTeacherName() {
    return classTeacherName;
  }

  public String getClassTeacherSignatureObjectKey() {
    return classTeacherSignatureObjectKey;
  }

  public void updateClassTeacher(String classTeacherName) {
    this.classTeacherName = classTeacherName;
  }

  public void changeClassTeacherSignature(String classTeacherSignatureObjectKey) {
    this.classTeacherSignatureObjectKey = classTeacherSignatureObjectKey;
  }

  public void update(String code, String displayName, short sortOrder, boolean archived) {
    this.code = code;
    this.displayName = displayName;
    this.sortOrder = sortOrder;
    this.isArchived = archived;
  }
}
