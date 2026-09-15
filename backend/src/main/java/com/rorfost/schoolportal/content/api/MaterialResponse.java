package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.content.domain.StudyMaterial;
import java.util.UUID;

public record MaterialResponse(
    UUID id,
    String title,
    String description,
    String materialType,
    UUID academicYearId,
    UUID standardSubjectId,
    String filename,
    String contentType,
    long byteSize,
    String url,
    String status) {
  public static MaterialResponse from(StudyMaterial item, String url) {
    return new MaterialResponse(
        item.getId(),
        item.getTitle(),
        item.getDescription(),
        item.getMaterialType(),
        item.getAcademicYearId(),
        item.getStandardSubjectId(),
        item.getOriginalFilename(),
        item.getContentType(),
        item.getByteSize(),
        url,
        item.getStatus().name());
  }
}
