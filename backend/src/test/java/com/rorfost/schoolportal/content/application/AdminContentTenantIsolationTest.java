package com.rorfost.schoolportal.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.content.api.DownloadMetadataRequest;
import com.rorfost.schoolportal.content.api.GalleryImageMetadataRequest;
import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.GalleryImageRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-tenant isolation tests for admin content operations.
 *
 * <p>Each test verifies that an authenticated admin from School A cannot affect or read data
 * belonging to School B, even when they supply valid School B UUIDs in request parameters.
 */
class AdminContentTenantIsolationTest {

    // School A – the authenticated session owner
    private final UUID schoolA = UUID.randomUUID();
    private final UUID actorA = UUID.randomUUID();

    // School B – a different tenant whose data must never be accessible by School A's admin
    private final UUID schoolB = UUID.randomUUID();

    private final UUID albumA = UUID.randomUUID();

    private final StorageService storage = Mockito.mock(StorageService.class);
    private final GalleryAlbumRepository albums = Mockito.mock(GalleryAlbumRepository.class);
    private final GalleryImageRepository images = Mockito.mock(GalleryImageRepository.class);
    private final SchoolRepository schools = Mockito.mock(SchoolRepository.class);
    private final AcademicYearRepository academicYears = Mockito.mock(AcademicYearRepository.class);
    private final DownloadRepository downloads = Mockito.mock(DownloadRepository.class);

    private GalleryAlbum album;
    private ContentService service;

    @BeforeEach
    void setUp() {
        service =
                new ContentService(
                        storage,
                        Mockito.mock(StudyMaterialRepository.class),
                        Mockito.mock(NoticeRepository.class),
                        albums,
                        images,
                        downloads,
                        schools,
                        academicYears,
                        Mockito.mock(StandardSubjectRepository.class),
                        Mockito.mock(StandardRepository.class),
                        Mockito.mock(SubjectRepository.class),
                        Mockito.mock(AuditLogService.class));

        album = new GalleryAlbum(schoolA, "Album A", null);
        setId(album, albumA);

        when(albums.findByIdAndSchoolIdForUpdate(albumA, schoolA)).thenReturn(Optional.of(album));
        when(albums.findByIdAndSchoolId(albumA, schoolA)).thenReturn(Optional.of(album));
        when(schools.findById(schoolA)).thenReturn(Optional.of(new School("School A", "school-a")));
        when(storage.publicUrl(anyString()))
                .thenAnswer(inv -> "https://assets/" + inv.getArgument(0));
        when(storage.publicImageThumbnailUrl(anyString()))
                .thenAnswer(inv -> "https://assets/thumb/" + inv.getArgument(0));
    }

    // -----------------------------------------------------------------------
    // Gallery image – cross-tenant image UUID returns 404
    // -----------------------------------------------------------------------

    @Test
    void setAlbumCoverRejectsForeignImageId() {
        UUID foreignImageId = UUID.randomUUID();
        // No image with (foreignImageId, albumA, schoolA) exists
        when(images.findByIdAndGalleryAlbumIdAndSchoolId(foreignImageId, albumA, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setAlbumCover(schoolA, actorA, albumA, foreignImageId))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("gallery_image_not_found"))
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getStatus())
                                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateImageMetadataRejectsForeignImageId() {
        UUID foreignImageId = UUID.randomUUID();
        when(images.findByIdAndGalleryAlbumIdAndSchoolId(foreignImageId, albumA, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.updateImageMetadata(
                                        schoolA,
                                        actorA,
                                        albumA,
                                        foreignImageId,
                                        new GalleryImageMetadataRequest("Alt", null)))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("gallery_image_not_found"));
    }

    @Test
    void deleteImageRejectsForeignImageId() {
        UUID foreignImageId = UUID.randomUUID();
        when(images.findByIdAndGalleryAlbumIdAndSchoolId(foreignImageId, albumA, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteImage(schoolA, actorA, albumA, foreignImageId))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("gallery_image_not_found"));

        // Storage must not be touched for a foreign image
        verify(storage, never()).delete(anyString(), anyString(), anyString());
    }

    @Test
    void setAlbumCoverRejectsForeignAlbumId() {
        UUID foreignAlbumId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        // School A admin supplies a School B album UUID – repo returns empty
        when(albums.findByIdAndSchoolIdForUpdate(foreignAlbumId, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setAlbumCover(schoolA, actorA, foreignAlbumId, imageId))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("gallery_album_not_found"));
    }

    // -----------------------------------------------------------------------
    // Download upload – foreign academicYearId is rejected before file is stored
    // -----------------------------------------------------------------------

    @Test
    void uploadDownloadRejectsForeignAcademicYearId() {
        UUID foreignYearId = UUID.randomUUID();
        AcademicYear foreignYear =
                new AcademicYear(schoolB, "2024-25", null, null, AcademicYearStatus.CURRENT, null);
        setId(foreignYear, foreignYearId);
        when(academicYears.findById(foreignYearId)).thenReturn(Optional.of(foreignYear));

        MockMultipartFile pdf =
                new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {0x25});
        DownloadMetadataRequest request =
                new DownloadMetadataRequest("Title", null, null, foreignYearId);

        assertThatThrownBy(() -> service.uploadDownload(schoolA, actorA, request, pdf))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("download_academic_year_invalid"))
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getStatus())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));

        // Storage upload must never be attempted when the scope check fails
        verify(storage, never()).uploadPublicDocument(anyString(), any());
    }

    @Test
    void uploadDownloadAcceptsOwnAcademicYearId() {
        UUID ownYearId = UUID.randomUUID();
        AcademicYear ownYear =
                new AcademicYear(schoolA, "2024-25", null, null, AcademicYearStatus.CURRENT, null);
        setId(ownYear, ownYearId);
        when(academicYears.findById(ownYearId)).thenReturn(Optional.of(ownYear));
        when(schools.findById(schoolA)).thenReturn(Optional.of(new School("School A", "school-a")));

        StoredObject object =
                new StoredObject(
                        "imagekit",
                        "downloads/school-a/doc.pdf",
                        "doc.pdf",
                        "application/pdf",
                        10,
                        "a".repeat(64),
                        "file-1");
        when(storage.uploadPublicDocument(anyString(), any())).thenReturn(object);
        when(downloads.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile pdf =
                new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {0x25});
        DownloadMetadataRequest request =
                new DownloadMetadataRequest("Title", null, null, ownYearId);

        // Must not throw – school A's own year is valid
        service.uploadDownload(schoolA, actorA, request, pdf);

        verify(storage).uploadPublicDocument(anyString(), any());
    }

    @Test
    void uploadDownloadAllowsNullAcademicYearId() {
        when(schools.findById(schoolA)).thenReturn(Optional.of(new School("School A", "school-a")));

        StoredObject object =
                new StoredObject(
                        "imagekit",
                        "downloads/school-a/doc.pdf",
                        "doc.pdf",
                        "application/pdf",
                        10,
                        "a".repeat(64),
                        "file-1");
        when(storage.uploadPublicDocument(anyString(), any())).thenReturn(object);
        when(downloads.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile pdf =
                new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {0x25});
        // academicYearId = null must be accepted without any repository lookup
        DownloadMetadataRequest request = new DownloadMetadataRequest("Title", null, null, null);

        service.uploadDownload(schoolA, actorA, request, pdf);

        verify(academicYears, never()).findById(any());
        verify(storage).uploadPublicDocument(anyString(), any());
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void setId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
