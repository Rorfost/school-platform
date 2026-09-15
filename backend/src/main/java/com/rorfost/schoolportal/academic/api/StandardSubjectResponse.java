package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.StandardSubject;
import java.util.UUID;

public record StandardSubjectResponse(UUID id, UUID standardId, UUID subjectId, short sortOrder) {
  public static StandardSubjectResponse from(StandardSubject value) {
    return new StandardSubjectResponse(
        value.getId(), value.getStandardId(), value.getSubjectId(), value.getSortOrder());
  }
}
