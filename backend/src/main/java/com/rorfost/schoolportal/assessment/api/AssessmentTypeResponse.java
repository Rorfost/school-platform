package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.domain.AssessmentType;
import java.util.UUID;

public record AssessmentTypeResponse(UUID id, String code, String displayName, short sortOrder) {
  public static AssessmentTypeResponse from(AssessmentType item) {
    return new AssessmentTypeResponse(
        item.getId(), item.getCode(), item.getDisplayName(), item.getSortOrder());
  }
}
