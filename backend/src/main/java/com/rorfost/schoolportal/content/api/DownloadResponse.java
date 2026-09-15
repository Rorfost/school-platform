package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.content.domain.Download;
import java.util.UUID;

public record DownloadResponse(
    UUID id,
    String title,
    String description,
    String category,
    UUID academicYearId,
    String filename,
    String contentType,
    long byteSize,
    String url,
    String status) {
  public static DownloadResponse from(Download item, String url) {
    return new DownloadResponse(
        item.getId(),
        item.getTitle(),
        item.getDescription(),
        item.getCategory(),
        item.getAcademicYearId(),
        item.getOriginalFilename(),
        item.getContentType(),
        item.getByteSize(),
        url,
        item.getStatus().name());
  }
}
