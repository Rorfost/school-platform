package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.content.domain.GalleryImage;
import java.util.UUID;

public record GalleryImageResponse(
    UUID id,
    String altText,
    String caption,
    int sortOrder,
    String url,
    String thumbnailUrl,
    String status) {
  public static GalleryImageResponse from(GalleryImage item, String url, String thumbnailUrl) {
    return new GalleryImageResponse(
        item.getId(),
        item.getAltText(),
        item.getCaption(),
        item.getSortOrder(),
        url,
        thumbnailUrl,
        item.getStatus().name());
  }
}
