package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryAlbumRepository extends JpaRepository<GalleryAlbum, UUID> {
  Page<GalleryAlbum> findBySchoolIdAndStatus(
      UUID schoolId, PublicationStatus status, Pageable pageable);

  java.util.Optional<GalleryAlbum> findByIdAndSchoolId(UUID id, UUID schoolId);

  long countBySchoolIdAndStatus(UUID schoolId, PublicationStatus status);
}
