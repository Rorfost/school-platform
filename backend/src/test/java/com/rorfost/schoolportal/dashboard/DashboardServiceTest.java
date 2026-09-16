package com.rorfost.schoolportal.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.dashboard.api.DashboardSummaryResponse;
import com.rorfost.schoolportal.dashboard.application.DashboardService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DashboardServiceTest {
  private final StudentRepository studentRepository = Mockito.mock(StudentRepository.class);
  private final StandardRepository standardRepository = Mockito.mock(StandardRepository.class);
  private final SubjectRepository subjectRepository = Mockito.mock(SubjectRepository.class);
  private final StudyMaterialRepository studyMaterialRepository =
      Mockito.mock(StudyMaterialRepository.class);
  private final NoticeRepository noticeRepository = Mockito.mock(NoticeRepository.class);
  private final GalleryAlbumRepository galleryAlbumRepository =
      Mockito.mock(GalleryAlbumRepository.class);
  private final DownloadRepository downloadRepository = Mockito.mock(DownloadRepository.class);

  @Test
  void computesDashboardSummaryCountsFromRepositories() {
    UUID schoolId = UUID.randomUUID();

    when(studentRepository.countBySchoolIdAndIsArchivedFalse(schoolId)).thenReturn(120L);
    when(standardRepository.countBySchoolIdAndIsArchivedFalse(schoolId)).thenReturn(8L);
    when(subjectRepository.countBySchoolIdAndIsArchivedFalse(schoolId)).thenReturn(12L);
    when(studyMaterialRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED))
        .thenReturn(15L);
    when(noticeRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED))
        .thenReturn(5L);
    when(galleryAlbumRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED))
        .thenReturn(3L);
    when(downloadRepository.countBySchoolId(schoolId)).thenReturn(10L);

    DashboardService service =
        new DashboardService(
            studentRepository,
            standardRepository,
            subjectRepository,
            studyMaterialRepository,
            noticeRepository,
            galleryAlbumRepository,
            downloadRepository);

    DashboardSummaryResponse summary = service.getSummary(schoolId);

    assertThat(summary.totalStudents()).isEqualTo(120L);
    assertThat(summary.totalStandards()).isEqualTo(8L);
    assertThat(summary.totalSubjects()).isEqualTo(12L);
    assertThat(summary.publishedMaterials()).isEqualTo(15L);
    assertThat(summary.publishedNotices()).isEqualTo(5L);
    assertThat(summary.publishedGalleryAlbums()).isEqualTo(3L);
    assertThat(summary.totalDownloads()).isEqualTo(10L);
  }
}
