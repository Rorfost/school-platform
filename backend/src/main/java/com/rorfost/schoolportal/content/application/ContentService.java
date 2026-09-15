package com.rorfost.schoolportal.content.application;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.content.api.DownloadMetadataRequest;
import com.rorfost.schoolportal.content.api.DownloadResponse;
import com.rorfost.schoolportal.content.api.GalleryAlbumRequest;
import com.rorfost.schoolportal.content.api.GalleryAlbumResponse;
import com.rorfost.schoolportal.content.api.GalleryImageResponse;
import com.rorfost.schoolportal.content.api.MaterialMetadataRequest;
import com.rorfost.schoolportal.content.api.MaterialResponse;
import com.rorfost.schoolportal.content.api.NoticeRequest;
import com.rorfost.schoolportal.content.api.NoticeResponse;
import com.rorfost.schoolportal.content.domain.Download;
import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.domain.StudyMaterial;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.GalleryImageRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentService {
  private final StorageService storage;
  private final StudyMaterialRepository materials;
  private final NoticeRepository notices;
  private final GalleryAlbumRepository albums;
  private final GalleryImageRepository images;
  private final DownloadRepository downloads;
  private final AuditLogService audit;

  public ContentService(
      StorageService storage,
      StudyMaterialRepository materials,
      NoticeRepository notices,
      GalleryAlbumRepository albums,
      GalleryImageRepository images,
      DownloadRepository downloads,
      AuditLogService audit) {
    this.storage = storage;
    this.materials = materials;
    this.notices = notices;
    this.albums = albums;
    this.images = images;
    this.downloads = downloads;
    this.audit = audit;
  }

  @Transactional
  public MaterialResponse uploadMaterial(
      UUID school, UUID actor, MaterialMetadataRequest request, MultipartFile file) {
    StoredObject object = storage.uploadPublicDocument("materials/" + school, file);
    try {
      StudyMaterial item =
          materials.saveAndFlush(
              new StudyMaterial(
                  school,
                  request.academicYearId(),
                  request.standardSubjectId(),
                  request.title().trim(),
                  trim(request.description()),
                  request.materialType().trim(),
                  object.bucket(),
                  object.objectKey(),
                  object.originalFilename(),
                  object.contentType(),
                  object.byteSize(),
                  object.checksumSha256()));
      audit(school, actor, AuditAction.MATERIAL_UPLOADED, "STUDY_MATERIAL", item.getId());
      return material(item);
    } catch (RuntimeException exception) {
      storage.delete(object);
      throw exception;
    }
  }

  @Transactional
  public NoticeResponse createNotice(UUID school, UUID actor, NoticeRequest request) {
    Notice item = notices.save(new Notice(school, request.title().trim(), request.body().trim()));
    item.update(
        request.title().trim(), request.body().trim(), request.pinned(), request.expiresAt());
    audit(school, actor, AuditAction.NOTICE_CREATED, "NOTICE", item.getId());
    return notice(item);
  }

  @Transactional
  public NoticeResponse attachNotice(UUID school, UUID actor, UUID id, MultipartFile file) {
    Notice item = noticeItem(school, id);
    StoredObject object = storage.uploadPublicDocument("notices/" + school, file);
    item.setAttachment(
        object.bucket(),
        object.objectKey(),
        object.originalFilename(),
        object.contentType(),
        object.byteSize());
    try {
      notices.saveAndFlush(item);
      audit(school, actor, AuditAction.NOTICE_UPDATED, "NOTICE", id);
      return notice(item);
    } catch (RuntimeException exception) {
      storage.delete(object);
      throw exception;
    }
  }

  @Transactional
  public GalleryAlbumResponse createAlbum(UUID school, UUID actor, GalleryAlbumRequest request) {
    GalleryAlbum item =
        albums.save(new GalleryAlbum(school, request.title().trim(), trim(request.description())));
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", item.getId());
    return GalleryAlbumResponse.from(item);
  }

  @Transactional
  public GalleryImageResponse uploadImage(
      UUID school,
      UUID actor,
      UUID albumId,
      String altText,
      String caption,
      int sortOrder,
      MultipartFile file) {
    GalleryAlbum album = album(school, albumId);
    if (album.getStatus() == PublicationStatus.ARCHIVED) throw conflict("gallery_album_archived");
    StoredObject object = storage.uploadPublicImage("gallery/" + school, file);
    try {
      GalleryImage item =
          images.saveAndFlush(
              new GalleryImage(
                  school,
                  albumId,
                  object.bucket(),
                  object.objectKey(),
                  object.originalFilename(),
                  object.contentType(),
                  object.byteSize(),
                  object.checksumSha256(),
                  altText.trim(),
                  trim(caption),
                  sortOrder));
      audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_IMAGE", item.getId());
      return image(item);
    } catch (RuntimeException exception) {
      storage.delete(object);
      throw exception;
    }
  }

  @Transactional
  public DownloadResponse uploadDownload(
      UUID school, UUID actor, DownloadMetadataRequest request, MultipartFile file) {
    StoredObject object = storage.uploadPublicDocument("downloads/" + school, file);
    try {
      Download item =
          downloads.saveAndFlush(
              new Download(
                  school,
                  request.academicYearId(),
                  request.title().trim(),
                  trim(request.description()),
                  trim(request.category()),
                  object.bucket(),
                  object.objectKey(),
                  object.originalFilename(),
                  object.contentType(),
                  object.byteSize(),
                  object.checksumSha256()));
      audit(school, actor, AuditAction.DOWNLOAD_UPLOADED, "DOWNLOAD", item.getId());
      return download(item);
    } catch (RuntimeException exception) {
      storage.delete(object);
      throw exception;
    }
  }

  @Transactional
  public void deleteMaterial(UUID school, UUID actor, UUID id) {
    StudyMaterial item =
        materials.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("material_not_found"));
    storage.delete(item.getStorageBucket(), item.getObjectKey());
    materials.delete(item);
    audit(school, actor, AuditAction.MATERIAL_DELETED, "STUDY_MATERIAL", id);
  }

  @Transactional
  public void deleteNotice(UUID school, UUID actor, UUID id) {
    Notice item = noticeItem(school, id);
    if (item.getAttachmentObjectKey() != null)
      storage.delete(item.getAttachmentBucket(), item.getAttachmentObjectKey());
    notices.delete(item);
    audit(school, actor, AuditAction.NOTICE_DELETED, "NOTICE", id);
  }

  @Transactional
  public void deleteDownload(UUID school, UUID actor, UUID id) {
    Download item =
        downloads.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("download_not_found"));
    storage.delete(item.getStorageBucket(), item.getObjectKey());
    downloads.delete(item);
    audit(school, actor, AuditAction.DOWNLOAD_DELETED, "DOWNLOAD", id);
  }

  @Transactional
  public void publishMaterial(UUID school, UUID actor, UUID id) {
    StudyMaterial item =
        materials.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("material_not_found"));
    item.publish(Instant.now());
    audit(school, actor, AuditAction.MATERIAL_UPLOADED, "STUDY_MATERIAL", id);
  }

  @Transactional
  public void publishNotice(UUID school, UUID actor, UUID id) {
    Notice item = noticeItem(school, id);
    item.publish(Instant.now());
    audit(school, actor, AuditAction.NOTICE_UPDATED, "NOTICE", id);
  }

  @Transactional
  public void publishAlbum(UUID school, UUID actor, UUID id) {
    GalleryAlbum item = album(school, id);
    item.publish(Instant.now());
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", id);
  }

  @Transactional
  public void publishDownload(UUID school, UUID actor, UUID id) {
    Download item =
        downloads.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("download_not_found"));
    item.publish(Instant.now());
    audit(school, actor, AuditAction.DOWNLOAD_UPLOADED, "DOWNLOAD", id);
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<StudyMaterial> publicMaterials(
      UUID school, int page, int size) {
    return materials.findBySchoolIdAndStatus(
        school,
        PublicationStatus.PUBLISHED,
        page(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<Notice> publicNotices(
      UUID school, int page, int size) {
    return notices.findVisibleBySchoolIdAndStatus(
        school,
        PublicationStatus.PUBLISHED,
        page(page, size, Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("publishedAt"))));
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<GalleryAlbum> publicAlbums(
      UUID school, int page, int size) {
    return albums.findBySchoolIdAndStatus(
        school,
        PublicationStatus.PUBLISHED,
        page(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public org.springframework.data.domain.Page<Download> publicDownloads(
      UUID school, int page, int size) {
    return downloads.findBySchoolIdAndStatus(
        school,
        PublicationStatus.PUBLISHED,
        page(page, size, Sort.by(Sort.Direction.DESC, "publishedAt")));
  }

  @Transactional(readOnly = true)
  public List<GalleryImageResponse> publicAlbumImages(UUID school, UUID albumId) {
    GalleryAlbum album = album(school, albumId);
    if (album.getStatus() != PublicationStatus.PUBLISHED) throw notFound("gallery_album_not_found");
    return albumImages(albumId);
  }

  private PageRequest page(int page, int size, Sort sort) {
    if (page < 0 || size < 1 || size > 50)
      throw new DomainException(HttpStatus.BAD_REQUEST, "page_invalid");
    return PageRequest.of(page, size, sort);
  }

  public MaterialResponse material(StudyMaterial item) {
    return MaterialResponse.from(
        item,
        item.getStatus() == PublicationStatus.PUBLISHED
            ? storage.publicUrl(item.getObjectKey())
            : null);
  }

  public NoticeResponse notice(Notice item) {
    return NoticeResponse.from(
        item,
        item.getStatus() == PublicationStatus.PUBLISHED && item.getAttachmentObjectKey() != null
            ? storage.publicUrl(item.getAttachmentObjectKey())
            : null);
  }

  public DownloadResponse download(Download item) {
    return DownloadResponse.from(
        item,
        item.getStatus() == PublicationStatus.PUBLISHED
            ? storage.publicUrl(item.getObjectKey())
            : null);
  }

  public GalleryImageResponse image(GalleryImage item) {
    return GalleryImageResponse.from(
        item,
        item.getStatus() == PublicationStatus.PUBLISHED
            ? storage.publicUrl(item.getObjectKey())
            : null);
  }

  public List<GalleryImageResponse> albumImages(UUID albumId) {
    return images.findByGalleryAlbumIdOrderBySortOrder(albumId).stream()
        .filter(item -> item.getStatus() == PublicationStatus.PUBLISHED)
        .map(this::image)
        .toList();
  }

  private Notice noticeItem(UUID school, UUID id) {
    return notices.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("notice_not_found"));
  }

  private GalleryAlbum album(UUID school, UUID id) {
    return albums
        .findByIdAndSchoolId(id, school)
        .orElseThrow(() -> notFound("gallery_album_not_found"));
  }

  private DomainException notFound(String code) {
    return new DomainException(HttpStatus.NOT_FOUND, code);
  }

  private DomainException conflict(String code) {
    return new DomainException(HttpStatus.CONFLICT, code);
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private void audit(UUID school, UUID actor, AuditAction action, String type, UUID id) {
    audit.record(school, actor, action, type, id, MDC.get("requestId"));
  }
}
