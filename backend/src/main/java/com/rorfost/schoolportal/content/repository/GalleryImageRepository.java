package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {
  List<GalleryImage> findByGalleryAlbumIdOrderBySortOrder(UUID galleryAlbumId);

  Optional<GalleryImage> findByIdAndGalleryAlbumId(UUID id, UUID galleryAlbumId);

  long countByGalleryAlbumId(UUID galleryAlbumId);

  long countByGalleryAlbumIdAndStatus(UUID galleryAlbumId, PublicationStatus status);

  @Query(
      "select coalesce(max(image.sortOrder), 0) from GalleryImage image "
          + "where image.galleryAlbumId = :galleryAlbumId")
  int maxSortOrderByGalleryAlbumId(UUID galleryAlbumId);
}
