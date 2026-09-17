package com.rorfost.schoolportal.content.api;

import java.util.UUID;

public record GalleryAlbumResponse(
    UUID id,
    String title,
    String description,
    UUID coverImageId,
    String coverImageThumbnailUrl,
    long imageCount,
    String status) {}
