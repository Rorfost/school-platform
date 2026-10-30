package com.rorfost.schoolportal.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.content.api.GalleryAlbumResponse;
import com.rorfost.schoolportal.content.api.MaterialResponse;
import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.domain.StudyMaterial;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumSummaryRow;
import com.rorfost.schoolportal.content.repository.GalleryImageRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialLabelRow;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests verifying that batch query methods in {@link ContentService} assemble pages in O(1)
 * database queries, eliminating N+1 query regression.
 */
class ContentBatchQueryNPlusOneTest {

    private final UUID schoolId = UUID.randomUUID();
    private final UUID otherSchoolId = UUID.randomUUID();

    private StorageService storage;
    private StudyMaterialRepository materials;
    private NoticeRepository notices;
    private GalleryAlbumRepository albums;
    private GalleryImageRepository images;
    private DownloadRepository downloads;
    private SchoolRepository schools;
    private AcademicYearRepository academicYears;
    private StandardSubjectRepository standardSubjects;
    private StandardRepository standards;
    private SubjectRepository subjects;
    private AuditLogService audit;

    private ContentService service;

    @BeforeEach
    void setUp() {
        storage = Mockito.mock(StorageService.class);
        materials = Mockito.mock(StudyMaterialRepository.class);
        notices = Mockito.mock(NoticeRepository.class);
        albums = Mockito.mock(GalleryAlbumRepository.class);
        images = Mockito.mock(GalleryImageRepository.class);
        downloads = Mockito.mock(DownloadRepository.class);
        schools = Mockito.mock(SchoolRepository.class);
        academicYears = Mockito.mock(AcademicYearRepository.class);
        standardSubjects = Mockito.mock(StandardSubjectRepository.class);
        standards = Mockito.mock(StandardRepository.class);
        subjects = Mockito.mock(SubjectRepository.class);
        audit = Mockito.mock(AuditLogService.class);

        when(storage.publicUrl(any()))
                .thenAnswer(inv -> "https://storage.local/" + inv.getArgument(0));
        when(storage.publicImageThumbnailUrl(any()))
                .thenAnswer(inv -> "https://storage.local/thumb/" + inv.getArgument(0));

        service =
                new ContentService(
                        storage,
                        materials,
                        notices,
                        albums,
                        images,
                        downloads,
                        schools,
                        academicYears,
                        standardSubjects,
                        standards,
                        subjects,
                        audit);
    }

    @Nested
    @DisplayName("Gallery Album batch assembly (N+1 elimination)")
    class GalleryAlbumBatchTests {

        @Test
        @DisplayName(
                "publicAlbumPage executes exactly 1 batch summary query for 3 albums instead of 2N queries")
        void publicAlbumPage_executesSingleBatchSummaryQuery() {
            GalleryAlbum album1 = newAlbum("Sports Day 2026");
            GalleryAlbum album2 = newAlbum("Science Fair 2026");
            GalleryAlbum album3 = newAlbum("Annual Concert 2026");
            List<GalleryAlbum> albumList = List.of(album1, album2, album3);
            Page<GalleryAlbum> albumPage = new PageImpl<>(albumList, PageRequest.of(0, 20), 3);

            when(albums.findBySchoolIdAndStatus(
                            eq(schoolId), eq(PublicationStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(albumPage);

            List<GalleryAlbumSummaryRow> summaryRows =
                    List.of(
                            new GalleryAlbumSummaryRow(album1.getId(), "covers/sports.jpg", 12L),
                            new GalleryAlbumSummaryRow(album2.getId(), "covers/science.jpg", 5L),
                            new GalleryAlbumSummaryRow(album3.getId(), null, 0L));
            when(images.findSummariesBySchoolIdAndAlbumIds(eq(schoolId), any()))
                    .thenReturn(summaryRows);

            Page<GalleryAlbumResponse> responsePage = service.publicAlbumPage(schoolId, 0, 20);

            assertThat(responsePage.getContent()).hasSize(3);

            GalleryAlbumResponse r1 = responsePage.getContent().get(0);
            assertThat(r1.id()).isEqualTo(album1.getId());
            assertThat(r1.coverImageThumbnailUrl())
                    .isEqualTo("https://storage.local/thumb/covers/sports.jpg");
            assertThat(r1.imageCount()).isEqualTo(12L);

            GalleryAlbumResponse r2 = responsePage.getContent().get(1);
            assertThat(r2.coverImageThumbnailUrl())
                    .isEqualTo("https://storage.local/thumb/covers/science.jpg");
            assertThat(r2.imageCount()).isEqualTo(5L);

            GalleryAlbumResponse r3 = responsePage.getContent().get(2);
            assertThat(r3.coverImageThumbnailUrl()).isNull();
            assertThat(r3.imageCount()).isEqualTo(0L);

            // Exactly 1 batch query called with the school's album IDs
            verify(images, times(1)).findSummariesBySchoolIdAndAlbumIds(eq(schoolId), any());

            // Zero per-album queries
            verify(images, never()).findByIdAndGalleryAlbumIdAndSchoolId(any(), any(), any());
            verify(images, never()).countBySchoolIdAndGalleryAlbumIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("adminAlbumPage handles empty page with zero batch image queries")
        void adminAlbumPage_emptyPage_skipsSummaryQuery() {
            Page<GalleryAlbum> emptyPage =
                    new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(albums.findBySchoolId(eq(schoolId), any(PageRequest.class))).thenReturn(emptyPage);

            Page<GalleryAlbumResponse> responsePage = service.adminAlbumPage(schoolId, 0, 20);

            assertThat(responsePage.getContent()).isEmpty();
            verify(images, never()).findSummariesBySchoolIdAndAlbumIds(any(), any());
        }

        @Test
        @DisplayName(
                "publicAlbumPage scopes batch query by schoolId to prevent cross-tenant exposure")
        void publicAlbumPage_scopedBySchoolId() {
            GalleryAlbum album = newAlbum("Graduation 2026");
            when(albums.findBySchoolIdAndStatus(
                            eq(schoolId), eq(PublicationStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(album), PageRequest.of(0, 20), 1));

            service.publicAlbumPage(schoolId, 0, 20);

            // Verifies the query was strictly scoped to schoolId, never otherSchoolId
            verify(images, times(1)).findSummariesBySchoolIdAndAlbumIds(eq(schoolId), any());
            verify(images, never()).findSummariesBySchoolIdAndAlbumIds(eq(otherSchoolId), any());
        }
    }

    @Nested
    @DisplayName("Study Material batch assembly (N+1 elimination)")
    class StudyMaterialBatchTests {

        @Test
        @DisplayName(
                "publicMaterialPage resolves standard and subject labels via single batch query")
        void publicMaterialPage_executesSingleBatchLabelQuery() {
            StudyMaterial m1 = newMaterial("Algebra Chapter 1");
            StudyMaterial m2 = newMaterial("World History Notes");
            Page<StudyMaterial> materialPage =
                    new PageImpl<>(List.of(m1, m2), PageRequest.of(0, 20), 2);

            when(materials.findBySchoolIdAndStatus(
                            eq(schoolId), eq(PublicationStatus.PUBLISHED), any(PageRequest.class)))
                    .thenReturn(materialPage);

            List<StudyMaterialLabelRow> labelRows =
                    List.of(
                            new StudyMaterialLabelRow(m1.getId(), "Grade 10", "Mathematics"),
                            new StudyMaterialLabelRow(m2.getId(), "Grade 9", "History"));
            when(materials.findLabelsBySchoolIdAndMaterialIds(eq(schoolId), any()))
                    .thenReturn(labelRows);

            Page<MaterialResponse> responsePage = service.publicMaterialPage(schoolId, 0, 20);

            assertThat(responsePage.getContent()).hasSize(2);
            MaterialResponse r1 = responsePage.getContent().get(0);
            assertThat(r1.title()).isEqualTo("Algebra Chapter 1");
            assertThat(r1.standardName()).isEqualTo("Grade 10");
            assertThat(r1.subjectName()).isEqualTo("Mathematics");

            MaterialResponse r2 = responsePage.getContent().get(1);
            assertThat(r2.standardName()).isEqualTo("Grade 9");
            assertThat(r2.subjectName()).isEqualTo("History");

            // Exactly 1 batch query called
            verify(materials, times(1)).findLabelsBySchoolIdAndMaterialIds(eq(schoolId), any());

            // Zero per-item lookups into standardSubjects, standards, or subjects repositories
            verify(standardSubjects, never()).findByIdAndSchoolId(any(), any());
            verify(standards, never()).findByIdAndSchoolId(any(), any());
            verify(subjects, never()).findByIdAndSchoolId(any(), any());
        }

        @Test
        @DisplayName("adminMaterialPage handles missing labels gracefully without throwing")
        void adminMaterialPage_handlesMissingLabelsGracefully() {
            StudyMaterial m1 = newMaterial("Unmapped Material");
            Page<StudyMaterial> materialPage =
                    new PageImpl<>(List.of(m1), PageRequest.of(0, 20), 1);

            when(materials.findBySchoolId(eq(schoolId), any(PageRequest.class)))
                    .thenReturn(materialPage);
            // Batch returns empty (e.g. standardSubjectId was null or row deleted)
            when(materials.findLabelsBySchoolIdAndMaterialIds(eq(schoolId), any()))
                    .thenReturn(Collections.emptyList());

            Page<MaterialResponse> responsePage = service.adminMaterialPage(schoolId, 0, 20);

            assertThat(responsePage.getContent()).hasSize(1);
            MaterialResponse r1 = responsePage.getContent().get(0);
            assertThat(r1.standardName()).isNull();
            assertThat(r1.subjectName()).isNull();
        }

        @Test
        @DisplayName("adminMaterialPage empty page skips batch query")
        void adminMaterialPage_emptyPage_skipsBatchQuery() {
            Page<StudyMaterial> emptyPage =
                    new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(materials.findBySchoolId(eq(schoolId), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Page<MaterialResponse> responsePage = service.adminMaterialPage(schoolId, 0, 20);

            assertThat(responsePage.getContent()).isEmpty();
            verify(materials, never()).findLabelsBySchoolIdAndMaterialIds(any(), any());
        }
    }

    private GalleryAlbum newAlbum(String title) {
        GalleryAlbum a = new GalleryAlbum(schoolId, title, null);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(a, "status", PublicationStatus.PUBLISHED);
        return a;
    }

    private StudyMaterial newMaterial(String title) {
        StudyMaterial m =
                new StudyMaterial(
                        schoolId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        title,
                        null,
                        "PDF",
                        "bucket",
                        "materials/" + UUID.randomUUID() + ".pdf",
                        "notes.pdf",
                        "application/pdf",
                        1024L,
                        "hash",
                        "file-id");
        ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(m, "status", PublicationStatus.PUBLISHED);
        return m;
    }
}
