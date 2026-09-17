package com.rorfost.schoolportal.content.application;

import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.content.api.DownloadMetadataRequest;
import com.rorfost.schoolportal.content.api.DownloadResponse;
import com.rorfost.schoolportal.content.api.GalleryAlbumRequest;
import com.rorfost.schoolportal.content.api.GalleryAlbumResponse;
import com.rorfost.schoolportal.content.api.GalleryAlbumUpdateRequest;
import com.rorfost.schoolportal.content.api.GalleryImageMetadataRequest;
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
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContentService {
  private static final Logger log = LoggerFactory.getLogger(ContentService.class);

  private final StorageService storage;
  private final StudyMaterialRepository materials;
  private final NoticeRepository notices;
  private final GalleryAlbumRepository albums;
  private final GalleryImageRepository images;
  private final DownloadRepository downloads;
  private final SchoolRepository schools;
  private final AcademicYearRepository academicYears;
  private final StandardSubjectRepository standardSubjects;
  private final AuditLogService audit;

  public ContentService(
      StorageService storage,
      StudyMaterialRepository materials,
      NoticeRepository notices,
      GalleryAlbumRepository albums,
      GalleryImageRepository images,
      DownloadRepository downloads,
      SchoolRepository schools,
      AcademicYearRepository academicYears,
      StandardSubjectRepository standardSubjects,
      AuditLogService audit) {
    this.storage = storage;
    this.materials = materials;
    this.notices = notices;
    this.albums = albums;
    this.images = images;
    this.downloads = downloads;
    this.schools = schools;
    this.academicYears = academicYears;
    this.standardSubjects = standardSubjects;
    this.audit = audit;
  }

  @Transactional
  public MaterialResponse uploadMaterial(
      UUID school, UUID actor, MaterialMetadataRequest request, MultipartFile file) {
    validateMaterialScope(school, request);
    StoredObject object = storage.uploadPublicDocument(storagePrefix("materials", school), file);
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
    StoredObject object = storage.uploadPublicDocument(storagePrefix("notices", school), file);
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
    if (request.coverImageId() != null) throw badRequest("gallery_cover_image_invalid");
    GalleryAlbum item =
        albums.save(new GalleryAlbum(school, request.title().trim(), trim(request.description())));
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", item.getId());
    return albumResponse(item, false);
  }

  @Transactional
  public GalleryImageResponse uploadImage(
      UUID school, UUID actor, UUID albumId, String altText, String caption, MultipartFile file) {
    return uploadImages(
            school,
            actor,
            albumId,
            java.util.Collections.singletonList(altText),
            java.util.Collections.singletonList(caption),
            java.util.Collections.singletonList(file))
        .get(0);
  }

  @Transactional
  public List<GalleryImageResponse> uploadImages(
      UUID school,
      UUID actor,
      UUID albumId,
      List<String> altTexts,
      List<String> captions,
      List<MultipartFile> files) {
    if (files == null || files.isEmpty()) throw badRequest("gallery_images_required");
    if (altTexts == null || altTexts.size() != files.size())
      throw badRequest("gallery_image_alt_texts_invalid");
    if (captions != null && captions.size() != files.size())
      throw badRequest("gallery_image_captions_invalid");

    GalleryAlbum album = editableAlbumForUpdate(school, albumId);
    int nextSortOrder = images.maxSortOrderByGalleryAlbumId(albumId) + 1;
    Instant publishedAt = album.getStatus() == PublicationStatus.PUBLISHED ? Instant.now() : null;
    List<StoredObject> storedObjects = new ArrayList<>();
    List<GalleryImage> uploaded = new ArrayList<>();
    try {
      for (int index = 0; index < files.size(); index++) {
        StoredObject object =
            storage.uploadPublicImage(storagePrefix("gallery", school), files.get(index));
        storedObjects.add(object);
        GalleryImage item =
            new GalleryImage(
                school,
                albumId,
                object.bucket(),
                object.objectKey(),
                object.originalFilename(),
                object.contentType(),
                object.byteSize(),
                object.checksumSha256(),
                requiredTrim(altTexts.get(index), "gallery_image_alt_text_required"),
                trim(captions == null ? null : captions.get(index)),
                nextSortOrder++);
        if (publishedAt != null) item.publish(publishedAt);
        uploaded.add(images.saveAndFlush(item));
      }
      ensureCover(album, albumId);
      uploaded.forEach(
          item ->
              audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_IMAGE", item.getId()));
      return uploaded.stream().map(this::adminImage).toList();
    } catch (RuntimeException exception) {
      deleteStoredObjects(storedObjects);
      throw exception;
    }
  }

  @Transactional
  public GalleryAlbumResponse updateAlbum(
      UUID school, UUID actor, UUID id, GalleryAlbumUpdateRequest request) {
    GalleryAlbum item = editableAlbumForUpdate(school, id);
    item.update(request.title().trim(), trim(request.description()));
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", id);
    return albumResponse(item, false);
  }

  @Transactional
  public GalleryAlbumResponse setAlbumCover(UUID school, UUID actor, UUID albumId, UUID imageId) {
    GalleryAlbum item = editableAlbumForUpdate(school, albumId);
    imageInAlbum(albumId, imageId);
    item.setCoverImageId(imageId);
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", albumId);
    return albumResponse(item, false);
  }

  @Transactional
  public GalleryImageResponse updateImageMetadata(
      UUID school, UUID actor, UUID albumId, UUID imageId, GalleryImageMetadataRequest request) {
    editableAlbumForUpdate(school, albumId);
    GalleryImage item = imageInAlbum(albumId, imageId);
    item.updateMetadata(
        requiredTrim(request.altText(), "gallery_image_alt_text_required"),
        trim(request.caption()));
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_IMAGE", imageId);
    return adminImage(item);
  }

  @Transactional
  public List<GalleryImageResponse> reorderImages(
      UUID school, UUID actor, UUID albumId, List<UUID> imageIds) {
    GalleryAlbum album = editableAlbumForUpdate(school, albumId);
    List<GalleryImage> albumImages = images.findByGalleryAlbumIdOrderBySortOrder(albumId);
    validateReorder(albumImages, imageIds);
    normalizeOrder(albumImages, imageIds);
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", album.getId());
    return imageIds.stream().map(id -> adminImage(imageInAlbum(albumId, id))).toList();
  }

  @Transactional
  public void deleteImage(UUID school, UUID actor, UUID albumId, UUID imageId) {
    GalleryAlbum album = editableAlbumForUpdate(school, albumId);
    GalleryImage item = imageInAlbum(albumId, imageId);
    List<GalleryImage> remaining =
        images.findByGalleryAlbumIdOrderBySortOrder(albumId).stream()
            .filter(image -> !image.getId().equals(imageId))
            .toList();
    if (imageId.equals(album.getCoverImageId())) {
      album.setCoverImageId(remaining.isEmpty() ? null : remaining.get(0).getId());
      albums.saveAndFlush(album);
    }
    storage.delete(item.getStorageBucket(), item.getObjectKey());
    images.delete(item);
    images.flush();
    normalizeOrder(remaining, remaining.stream().map(GalleryImage::getId).toList());
    audit(school, actor, AuditAction.GALLERY_DELETED, "GALLERY_IMAGE", imageId);
  }

  @Transactional
  public void deleteAlbum(UUID school, UUID actor, UUID albumId) {
    GalleryAlbum album = albumForUpdate(school, albumId);
    List<GalleryImage> albumImages = images.findByGalleryAlbumIdOrderBySortOrder(albumId);
    for (GalleryImage image : albumImages)
      storage.delete(image.getStorageBucket(), image.getObjectKey());
    album.setCoverImageId(null);
    albums.saveAndFlush(album);
    images.deleteAll(albumImages);
    images.flush();
    albums.delete(album);
    audit(school, actor, AuditAction.GALLERY_DELETED, "GALLERY_ALBUM", albumId);
  }

  @Transactional
  public DownloadResponse uploadDownload(
      UUID school, UUID actor, DownloadMetadataRequest request, MultipartFile file) {
    StoredObject object = storage.uploadPublicDocument(storagePrefix("downloads", school), file);
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
    GalleryAlbum item = editableAlbumForUpdate(school, id);
    Instant publishedAt = Instant.now();
    item.publish(publishedAt);
    images.findByGalleryAlbumIdOrderBySortOrder(id).stream()
        .filter(image -> image.getStatus() != PublicationStatus.ARCHIVED)
        .forEach(image -> image.publish(publishedAt));
    audit(school, actor, AuditAction.GALLERY_UPLOADED, "GALLERY_ALBUM", id);
  }

  @Transactional
  public void unpublishAlbum(UUID school, UUID actor, UUID id) {
    GalleryAlbum item = editableAlbumForUpdate(school, id);
    item.unpublish();
    images.findByGalleryAlbumIdOrderBySortOrder(id).stream()
        .filter(image -> image.getStatus() == PublicationStatus.PUBLISHED)
        .forEach(GalleryImage::unpublish);
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
  public org.springframework.data.domain.Page<StudyMaterial> adminMaterials(
      UUID school, int page, int size) {
    return materials.findBySchoolId(
        school, page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
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
  public org.springframework.data.domain.Page<Notice> adminNotices(
      UUID school, int page, int size) {
    return notices.findBySchoolId(
        school, page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
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
  public org.springframework.data.domain.Page<GalleryAlbum> adminAlbums(
      UUID school, int page, int size) {
    return albums.findBySchoolId(
        school, page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
  }

  public GalleryAlbumResponse adminAlbum(GalleryAlbum album) {
    return albumResponse(album, false);
  }

  public GalleryAlbumResponse publicAlbum(GalleryAlbum album) {
    return albumResponse(album, true);
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
  public org.springframework.data.domain.Page<Download> adminDownloads(
      UUID school, int page, int size) {
    return downloads.findBySchoolId(
        school, page(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
  }

  @Transactional(readOnly = true)
  public List<GalleryImageResponse> publicAlbumImages(UUID school, UUID albumId) {
    GalleryAlbum album = album(school, albumId);
    if (album.getStatus() != PublicationStatus.PUBLISHED) throw notFound("gallery_album_not_found");
    return albumImages(albumId);
  }

  @Transactional(readOnly = true)
  public List<GalleryImageResponse> adminAlbumImages(UUID school, UUID albumId) {
    album(school, albumId);
    return images.findByGalleryAlbumIdOrderBySortOrder(albumId).stream()
        .map(this::adminImage)
        .toList();
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
            : null,
        item.getStatus() == PublicationStatus.PUBLISHED
            ? storage.publicImageThumbnailUrl(item.getObjectKey())
            : null);
  }

  public List<GalleryImageResponse> albumImages(UUID albumId) {
    return images.findByGalleryAlbumIdOrderBySortOrder(albumId).stream()
        .filter(item -> item.getStatus() == PublicationStatus.PUBLISHED)
        .map(this::image)
        .toList();
  }

  private GalleryImageResponse adminImage(GalleryImage item) {
    String url = storage.publicUrl(item.getObjectKey());
    return GalleryImageResponse.from(
        item, url, storage.publicImageThumbnailUrl(item.getObjectKey()));
  }

  private GalleryAlbumResponse albumResponse(GalleryAlbum album, boolean publicView) {
    String coverImageThumbnailUrl = null;
    if (album.getCoverImageId() != null) {
      GalleryImage cover =
          images.findByIdAndGalleryAlbumId(album.getCoverImageId(), album.getId()).orElse(null);
      if (cover != null && (!publicView || cover.getStatus() == PublicationStatus.PUBLISHED)) {
        coverImageThumbnailUrl = storage.publicImageThumbnailUrl(cover.getObjectKey());
      }
    }
    return new GalleryAlbumResponse(
        album.getId(),
        album.getTitle(),
        album.getDescription(),
        album.getCoverImageId(),
        coverImageThumbnailUrl,
        publicView
            ? images.countByGalleryAlbumIdAndStatus(album.getId(), PublicationStatus.PUBLISHED)
            : images.countByGalleryAlbumId(album.getId()),
        album.getStatus().name());
  }

  private Notice noticeItem(UUID school, UUID id) {
    return notices.findByIdAndSchoolId(id, school).orElseThrow(() -> notFound("notice_not_found"));
  }

  private GalleryAlbum album(UUID school, UUID id) {
    return albums
        .findByIdAndSchoolId(id, school)
        .orElseThrow(() -> notFound("gallery_album_not_found"));
  }

  private GalleryAlbum albumForUpdate(UUID school, UUID id) {
    return albums
        .findByIdAndSchoolIdForUpdate(id, school)
        .orElseThrow(() -> notFound("gallery_album_not_found"));
  }

  private GalleryAlbum editableAlbumForUpdate(UUID school, UUID id) {
    GalleryAlbum item = albumForUpdate(school, id);
    if (item.getStatus() == PublicationStatus.ARCHIVED) throw conflict("gallery_album_archived");
    return item;
  }

  private GalleryImage imageInAlbum(UUID albumId, UUID imageId) {
    return images
        .findByIdAndGalleryAlbumId(imageId, albumId)
        .orElseThrow(() -> notFound("gallery_image_not_found"));
  }

  private void ensureCover(GalleryAlbum album, UUID albumId) {
    if (album.getCoverImageId() != null) return;
    images.findByGalleryAlbumIdOrderBySortOrder(albumId).stream()
        .findFirst()
        .ifPresent(image -> album.setCoverImageId(image.getId()));
  }

  private void validateReorder(List<GalleryImage> albumImages, List<UUID> imageIds) {
    if (imageIds == null || imageIds.isEmpty()) throw badRequest("gallery_image_ids_required");
    Set<UUID> requested = new HashSet<>(imageIds);
    if (requested.size() != imageIds.size()) throw badRequest("gallery_image_ids_duplicate");
    if (requested.size() != albumImages.size()) throw badRequest("gallery_image_ids_invalid");
    Set<UUID> existing =
        albumImages.stream().map(GalleryImage::getId).collect(java.util.stream.Collectors.toSet());
    if (!existing.equals(requested)) throw badRequest("gallery_image_ids_invalid");
  }

  private void normalizeOrder(List<GalleryImage> albumImages, List<UUID> orderedImageIds) {
    if (albumImages.isEmpty()) return;
    Map<UUID, GalleryImage> byId = new HashMap<>();
    int largestOrder = 0;
    for (GalleryImage image : albumImages) {
      byId.put(image.getId(), image);
      largestOrder = Math.max(largestOrder, image.getSortOrder());
    }
    int temporaryBase = largestOrder + albumImages.size();
    for (int index = 0; index < albumImages.size(); index++) {
      albumImages.get(index).setSortOrder(temporaryBase + index + 1);
    }
    images.flush();
    for (int index = 0; index < orderedImageIds.size(); index++) {
      byId.get(orderedImageIds.get(index)).setSortOrder(index + 1);
    }
    images.flush();
  }

  private void deleteStoredObjects(List<StoredObject> storedObjects) {
    for (StoredObject object : storedObjects) {
      try {
        storage.delete(object);
      } catch (RuntimeException cleanupFailure) {
        log.error(
            "Could not clean up gallery object after a failed upload: {}",
            object.objectKey(),
            cleanupFailure);
      }
    }
  }

  private DomainException notFound(String code) {
    return new DomainException(HttpStatus.NOT_FOUND, code);
  }

  private DomainException conflict(String code) {
    return new DomainException(HttpStatus.CONFLICT, code);
  }

  private DomainException badRequest(String code) {
    return new DomainException(HttpStatus.BAD_REQUEST, code);
  }

  private String storagePrefix(String category, UUID schoolId) {
    String schoolSlug =
        schools
            .findById(schoolId)
            .map(school -> school.getSlug())
            .orElseThrow(() -> notFound("school_not_found"));
    return category + "/" + schoolSlug;
  }

  private void validateMaterialScope(UUID schoolId, MaterialMetadataRequest request) {
    if (request.academicYearId() != null
        && academicYears
            .findById(request.academicYearId())
            .filter(year -> year.getSchoolId().equals(schoolId))
            .isEmpty()) {
      throw badRequest("material_academic_year_invalid");
    }
    // Multipart field values are user-controlled; the backend owns the relational mapping boundary.
    if (request.standardSubjectId() != null
        && standardSubjects.findByIdAndSchoolId(request.standardSubjectId(), schoolId).isEmpty()) {
      throw badRequest("material_standard_subject_invalid");
    }
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private String requiredTrim(String value, String code) {
    String trimmed = trim(value);
    if (trimmed == null || trimmed.isEmpty()) throw badRequest(code);
    if (trimmed.length() > 255) throw badRequest("gallery_image_alt_text_too_long");
    return trimmed;
  }

  private void audit(UUID school, UUID actor, AuditAction action, String type, UUID id) {
    audit.record(school, actor, action, type, id, MDC.get("requestId"));
  }
}
