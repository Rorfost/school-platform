package com.rorfost.schoolportal.content.repository;

import java.util.UUID;

/**
 * Projection carrying per-album summary data fetched in a single batch query.
 *
 * <p>Used by {@code ContentService.albumsWithSummary} to eliminate N+1 queries when assembling
 * gallery album list responses. Each row provides:
 *
 * <ul>
 *   <li>{@link #albumId()} — the gallery album UUID (join key back to the album page).
 *   <li>{@link #coverObjectKey()} — the storage key of the designated cover image, {@code null} if
 *       no cover is configured or the cover is not in the requested status.
 *   <li>{@link #publishedImageCount()} — count of images with {@code PUBLISHED} status, used for
 *       both the public and admin list views.
 * </ul>
 */
public record GalleryAlbumSummaryRow(
        UUID albumId, String coverObjectKey, long publishedImageCount) {}
