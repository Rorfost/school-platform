package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.Subject;
import java.util.UUID;

public record SubjectResponse(
    UUID id, String code, String name, short sortOrder, boolean archived) {
  public static SubjectResponse from(Subject value) {
    return new SubjectResponse(
        value.getId(), value.getCode(), value.getName(), value.getSortOrder(), value.isArchived());
  }
}
