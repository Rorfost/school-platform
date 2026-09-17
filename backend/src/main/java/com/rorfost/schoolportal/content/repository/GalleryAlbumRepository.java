package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GalleryAlbumRepository extends JpaRepository<GalleryAlbum, UUID> {
  Page<GalleryAlbum> findBySchoolIdAndStatus(
      UUID schoolId, PublicationStatus status, Pageable pageable);

  Page<GalleryAlbum> findBySchoolId(UUID schoolId, Pageable pageable);

  java.util.Optional<GalleryAlbum> findByIdAndSchoolId(UUID id, UUID schoolId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select album from GalleryAlbum album where album.id = :id and album.schoolId = :schoolId")
  java.util.Optional<GalleryAlbum> findByIdAndSchoolIdForUpdate(
      @Param("id") UUID id, @Param("schoolId") UUID schoolId);

  long countBySchoolIdAndStatus(UUID schoolId, PublicationStatus status);
}
