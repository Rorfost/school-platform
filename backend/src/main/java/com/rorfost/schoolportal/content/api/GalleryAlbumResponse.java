package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import java.util.UUID;

public record GalleryAlbumResponse(
    UUID id, String title, String description, UUID coverImageId, String status) {
  public static GalleryAlbumResponse from(GalleryAlbum item) {
    return new GalleryAlbumResponse(
        item.getId(),
        item.getTitle(),
        item.getDescription(),
        item.getCoverImageId(),
        item.getStatus().name());
  }
}
