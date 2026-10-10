package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.List;
import java.util.UUID;

public record StandardResponse(
    UUID id,
    String code,
    String displayName,
    short sortOrder,
    boolean archived,
    String classTeacherName,
    String classTeacherSignatureObjectKey,
    String classTeacherSignatureUrl,
    List<String> classes) {

  public static StandardResponse from(Standard value) {
    return from(value, null, List.of());
  }

  public static StandardResponse from(Standard value, String classTeacherSignatureUrl) {
    return from(value, classTeacherSignatureUrl, List.of());
  }

  public static StandardResponse from(
      Standard value, String classTeacherSignatureUrl, List<String> classes) {
    return new StandardResponse(
        value.getId(),
        value.getCode(),
        value.getDisplayName(),
        value.getSortOrder(),
        value.isArchived(),
        value.getClassTeacherName(),
        value.getClassTeacherSignatureObjectKey(),
        classTeacherSignatureUrl,
        classes == null ? List.of() : classes);
  }
}
