package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.common.api.PageResponse;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
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
    private final FeatureAccessPolicy featureAccessPolicy;

    PublicContentController(
            ContentService contentService,
            SchoolRepository schoolRepository,
            FeatureAccessPolicy featureAccessPolicy) {
        this.contentService = contentService;
        this.schoolRepository = schoolRepository;
        this.featureAccessPolicy = featureAccessPolicy;
    }

    @GetMapping("/materials")
    PageResponse<MaterialResponse> materials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.MATERIALS);
        return PageResponse.from(contentService.publicMaterialPage(schoolId, page, size));
    }

    @GetMapping("/notices")
    PageResponse<NoticeResponse> notices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.NOTICES);
        return PageResponse.from(
                contentService.publicNotices(schoolId, page, size).map(contentService::notice));
    }

    @GetMapping("/gallery/albums")
    PageResponse<GalleryAlbumResponse> galleryAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.GALLERY);
        return PageResponse.from(contentService.publicAlbumPage(schoolId, page, size));
    }

    @GetMapping("/gallery/albums/{albumId}/images")
    Object galleryImages(
            @PathVariable UUID albumId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.GALLERY);
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            return PageResponse.from(contentService.publicAlbumImagesPage(schoolId, albumId, p, s));
        }
        return contentService.publicAlbumImages(schoolId, albumId);
    }

    List<GalleryImageResponse> galleryImages(UUID albumId) {
        return (List<GalleryImageResponse>) galleryImages(albumId, null, null);
    }

    @GetMapping("/downloads")
    PageResponse<DownloadResponse> downloads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.DOWNLOADS);
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
