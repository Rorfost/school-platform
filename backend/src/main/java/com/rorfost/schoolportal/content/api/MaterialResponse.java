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
    String standardName,
    String subjectName,
    String filename,
    String contentType,
    long byteSize,
    String url,
    String status) {
  public static MaterialResponse from(
      StudyMaterial item, String url, String standardName, String subjectName) {
    return new MaterialResponse(
        item.getId(),
        item.getTitle(),
        item.getDescription(),
        item.getMaterialType(),
        item.getAcademicYearId(),
        item.getStandardSubjectId(),
        standardName,
        subjectName,
        item.getOriginalFilename(),
        item.getContentType(),
        item.getByteSize(),
        url,
        item.getStatus().name());
  }
}
