package com.rorfost.schoolportal.dashboard.application;

import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.dashboard.api.DashboardSummaryResponse;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {
  private final StudentRepository studentRepository;
  private final StandardRepository standardRepository;
  private final SubjectRepository subjectRepository;
  private final StudyMaterialRepository studyMaterialRepository;
  private final NoticeRepository noticeRepository;
  private final GalleryAlbumRepository galleryAlbumRepository;
  private final DownloadRepository downloadRepository;

  public DashboardService(
      StudentRepository studentRepository,
      StandardRepository standardRepository,
      SubjectRepository subjectRepository,
      StudyMaterialRepository studyMaterialRepository,
      NoticeRepository noticeRepository,
      GalleryAlbumRepository galleryAlbumRepository,
      DownloadRepository downloadRepository) {
    this.studentRepository = studentRepository;
    this.standardRepository = standardRepository;
    this.subjectRepository = subjectRepository;
    this.studyMaterialRepository = studyMaterialRepository;
    this.noticeRepository = noticeRepository;
    this.galleryAlbumRepository = galleryAlbumRepository;
    this.downloadRepository = downloadRepository;
  }

  public DashboardSummaryResponse getSummary(UUID schoolId) {
    long totalStudents = studentRepository.countBySchoolIdAndIsArchivedFalse(schoolId);
    long totalStandards = standardRepository.countBySchoolIdAndIsArchivedFalse(schoolId);
    long totalSubjects = subjectRepository.countBySchoolIdAndIsArchivedFalse(schoolId);
    long publishedMaterials =
        studyMaterialRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED);
    long publishedNotices =
        noticeRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED);
    long publishedGalleryAlbums =
        galleryAlbumRepository.countBySchoolIdAndStatus(schoolId, PublicationStatus.PUBLISHED);
    long totalDownloads = downloadRepository.countBySchoolId(schoolId);

    return new DashboardSummaryResponse(
        totalStudents,
        totalStandards,
        totalSubjects,
        publishedMaterials,
        publishedNotices,
        publishedGalleryAlbums,
        totalDownloads);
  }
}
