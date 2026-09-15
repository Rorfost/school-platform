package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.UUID;

public record StandardResponse(
    UUID id, String code, String displayName, short sortOrder, boolean archived) {
  public static StandardResponse from(Standard value) {
    return new StandardResponse(
        value.getId(),
        value.getCode(),
        value.getDisplayName(),
        value.getSortOrder(),
        value.isArchived());
  }
}
