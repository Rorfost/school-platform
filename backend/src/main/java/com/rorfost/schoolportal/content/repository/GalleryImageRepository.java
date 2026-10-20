package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.PublicationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /**
     * Batch query that returns one {@link GalleryAlbumSummaryRow} per requested album.
     *
     * <p>A single SQL replaces the two-query-per-album pattern (cover image lookup + published
     * count) that caused N+1 when rendering album list pages. Both subqueries are scoped to {@code
     * schoolId} so cross-tenant rows can never leak through.
     *
     * <p>The cover object key is fetched via a correlated subquery so that only the image whose
     * {@code id = album.coverImageId} and {@code school_id = schoolId} is returned — this matches
     * the defence-in-depth check previously performed by {@code
     * findByIdAndGalleryAlbumIdAndSchoolId} but in one round-trip for the full page.
     *
     * <p>Albums with no cover image return {@code null} for {@code coverObjectKey}. Albums with
     * zero published images return {@code 0} for {@code publishedImageCount}.
     */
    @Query(
            """
            select new com.rorfost.schoolportal.content.repository.GalleryAlbumSummaryRow(
                album.id,
                (select img.objectKey from GalleryImage img
                 where img.id = album.coverImageId
                   and img.galleryAlbumId = album.id
                   and img.schoolId = :schoolId
                   and img.status = 'PUBLISHED'),
                (select count(img2) from GalleryImage img2
                 where img2.galleryAlbumId = album.id
                   and img2.schoolId = :schoolId
                   and img2.status = 'PUBLISHED')
            )
            from GalleryAlbum album
            where album.id in :albumIds
              and album.schoolId = :schoolId
            """)
    List<GalleryAlbumSummaryRow> findSummariesBySchoolIdAndAlbumIds(
            @Param("schoolId") UUID schoolId, @Param("albumIds") Collection<UUID> albumIds);
}
