package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.UUID;

public record StandardResponse(
    UUID id,
    String code,
    String displayName,
    short sortOrder,
    boolean archived,
    String classTeacherName,
    String classTeacherSignatureObjectKey,
    String classTeacherSignatureUrl) {
  public static StandardResponse from(Standard value) {
    return from(value, null);
  }

  public static StandardResponse from(Standard value, String classTeacherSignatureUrl) {
    return new StandardResponse(
        value.getId(),
        value.getCode(),
        value.getDisplayName(),
        value.getSortOrder(),
        value.isArchived(),
        value.getClassTeacherName(),
        value.getClassTeacherSignatureObjectKey(),
        classTeacherSignatureUrl);
  }
}
