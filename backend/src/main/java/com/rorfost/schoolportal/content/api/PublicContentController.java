package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.common.api.PageResponse;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
class PublicContentController {
    private final ContentService contentService;
    private final SchoolRepository schoolRepository;

    PublicContentController(ContentService contentService, SchoolRepository schoolRepository) {
        this.contentService = contentService;
        this.schoolRepository = schoolRepository;
    }

    @GetMapping("/materials")
    PageResponse<MaterialResponse> materials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        return PageResponse.from(
                contentService.publicMaterials(schoolId, page, size).map(contentService::material));
    }

    @GetMapping("/notices")
    PageResponse<NoticeResponse> notices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        return PageResponse.from(
                contentService.publicNotices(schoolId, page, size).map(contentService::notice));
    }

    @GetMapping("/gallery/albums")
    PageResponse<GalleryAlbumResponse> galleryAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        return PageResponse.from(
                contentService.publicAlbums(schoolId, page, size).map(contentService::publicAlbum));
    }

    @GetMapping("/gallery/albums/{albumId}/images")
    List<GalleryImageResponse> galleryImages(@PathVariable UUID albumId) {
        UUID schoolId = resolveTenantSchoolId();
        return contentService.publicAlbumImages(schoolId, albumId);
    }

    @GetMapping("/downloads")
    PageResponse<DownloadResponse> downloads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        return PageResponse.from(
                contentService.publicDownloads(schoolId, page, size).map(contentService::download));
    }

    private UUID resolveTenantSchoolId() {
        UUID schoolId =
                TenantContextHolder.getCurrentTenant()
                        .map(TenantContext::schoolId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "school_not_found"));
        return schoolRepository
                .findByIdAndIsActiveTrue(schoolId)
                .map(School::getId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
    }
}
