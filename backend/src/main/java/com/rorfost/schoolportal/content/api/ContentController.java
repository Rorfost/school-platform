package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.content.application.ContentService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
class ContentController {
  private final ContentService service;

  ContentController(ContentService service) {
    this.service = service;
  }

  @PostMapping(value = "/materials", consumes = "multipart/form-data")
  MaterialResponse material(
      @AuthenticationPrincipal PrincipalSession p,
      @RequestParam String title,
      @RequestParam String materialType,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) UUID academicYearId,
      @RequestParam(required = false) UUID standardSubjectId,
      @RequestParam(required = false) String description) {
    return service.uploadMaterial(
        p.schoolId(),
        p.adminUserId(),
        new MaterialMetadataRequest(
            title, description, materialType, academicYearId, standardSubjectId),
        file);
  }

  @PostMapping("/materials/{id}/publish")
  void publishMaterial(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.publishMaterial(p.schoolId(), p.adminUserId(), id);
  }

  @DeleteMapping("/materials/{id}")
  void deleteMaterial(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.deleteMaterial(p.schoolId(), p.adminUserId(), id);
  }

  @PostMapping("/notices")
  NoticeResponse notice(
      @AuthenticationPrincipal PrincipalSession p,
      @Valid @org.springframework.web.bind.annotation.RequestBody NoticeRequest r) {
    return service.createNotice(p.schoolId(), p.adminUserId(), r);
  }

  @PostMapping(value = "/notices/{id}/attachment", consumes = "multipart/form-data")
  NoticeResponse attachment(
      @AuthenticationPrincipal PrincipalSession p,
      @PathVariable UUID id,
      @RequestParam MultipartFile file) {
    return service.attachNotice(p.schoolId(), p.adminUserId(), id, file);
  }

  @PostMapping("/notices/{id}/publish")
  void publishNotice(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.publishNotice(p.schoolId(), p.adminUserId(), id);
  }

  @DeleteMapping("/notices/{id}")
  void deleteNotice(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.deleteNotice(p.schoolId(), p.adminUserId(), id);
  }

  @PostMapping("/gallery/albums")
  GalleryAlbumResponse album(
      @AuthenticationPrincipal PrincipalSession p,
      @Valid @org.springframework.web.bind.annotation.RequestBody GalleryAlbumRequest r) {
    return service.createAlbum(p.schoolId(), p.adminUserId(), r);
  }

  @PostMapping(value = "/gallery/albums/{id}/images", consumes = "multipart/form-data")
  GalleryImageResponse image(
      @AuthenticationPrincipal PrincipalSession p,
      @PathVariable UUID id,
      @RequestParam String altText,
      @RequestParam int sortOrder,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String caption) {
    return service.uploadImage(
        p.schoolId(), p.adminUserId(), id, altText, caption, sortOrder, file);
  }

  @PostMapping("/gallery/albums/{id}/publish")
  void publishAlbum(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.publishAlbum(p.schoolId(), p.adminUserId(), id);
  }

  @PostMapping(value = "/downloads", consumes = "multipart/form-data")
  DownloadResponse download(
      @AuthenticationPrincipal PrincipalSession p,
      @RequestParam String title,
      @RequestParam MultipartFile file,
      @RequestParam(required = false) String description,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) UUID academicYearId) {
    return service.uploadDownload(
        p.schoolId(),
        p.adminUserId(),
        new DownloadMetadataRequest(title, description, category, academicYearId),
        file);
  }

  @PostMapping("/downloads/{id}/publish")
  void publishDownload(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.publishDownload(p.schoolId(), p.adminUserId(), id);
  }

  @DeleteMapping("/downloads/{id}")
  void deleteDownload(@AuthenticationPrincipal PrincipalSession p, @PathVariable UUID id) {
    service.deleteDownload(p.schoolId(), p.adminUserId(), id);
  }
}
