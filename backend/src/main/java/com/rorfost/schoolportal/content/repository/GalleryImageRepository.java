package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {
  List<GalleryImage> findByGalleryAlbumIdOrderBySortOrder(UUID galleryAlbumId);

  Optional<GalleryImage> findByIdAndGalleryAlbumId(UUID id, UUID galleryAlbumId);
}
