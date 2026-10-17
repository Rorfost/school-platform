package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.PublicationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalleryImageRepository extends JpaRepository<GalleryImage, UUID> {
    List<GalleryImage> findByGalleryAlbumIdOrderBySortOrder(UUID galleryAlbumId);

    Optional<GalleryImage> findByIdAndGalleryAlbumId(UUID id, UUID galleryAlbumId);

    long countByGalleryAlbumId(UUID galleryAlbumId);

    long countByGalleryAlbumIdAndStatus(UUID galleryAlbumId, PublicationStatus status);

    List<GalleryImage> findBySchoolIdAndGalleryAlbumIdAndStatusOrderBySortOrder(
            UUID schoolId, UUID galleryAlbumId, PublicationStatus status);

    Optional<GalleryImage> findByIdAndGalleryAlbumIdAndSchoolId(
            UUID id, UUID galleryAlbumId, UUID schoolId);

    long countBySchoolIdAndGalleryAlbumId(UUID schoolId, UUID galleryAlbumId);

    long countBySchoolIdAndGalleryAlbumIdAndStatus(
            UUID schoolId, UUID galleryAlbumId, PublicationStatus status);

    @Query(
            "select coalesce(max(image.sortOrder), 0) from GalleryImage image "
                    + "where image.galleryAlbumId = :galleryAlbumId")
    int maxSortOrderByGalleryAlbumId(UUID galleryAlbumId);
}
