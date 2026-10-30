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
public class PublicContentController {
    private final ContentService contentService;
    private final SchoolRepository schoolRepository;
    private final FeatureAccessPolicy featureAccessPolicy;

    public PublicContentController(
            ContentService contentService,
            SchoolRepository schoolRepository,
            FeatureAccessPolicy featureAccessPolicy) {
        this.contentService = contentService;
        this.schoolRepository = schoolRepository;
        this.featureAccessPolicy = featureAccessPolicy;
    }

    private static final org.springframework.http.CacheControl PUBLIC_CONTENT_CACHE =
            org.springframework.http.CacheControl.maxAge(java.time.Duration.ofSeconds(60))
                    .cachePublic()
                    .sMaxAge(java.time.Duration.ofSeconds(60));

    @GetMapping("/materials")
    org.springframework.http.ResponseEntity<PageResponse<MaterialResponse>> materials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.MATERIALS);
        PageResponse<MaterialResponse> response =
                PageResponse.from(contentService.publicMaterialPage(schoolId, page, size));
        String etag =
                "\""
                        + schoolId
                        + ":materials:"
                        + page
                        + ":"
                        + size
                        + ":"
                        + response.hashCode()
                        + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(response);
    }

    @GetMapping("/notices")
    org.springframework.http.ResponseEntity<PageResponse<NoticeResponse>> notices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.NOTICES);
        PageResponse<NoticeResponse> response =
                PageResponse.from(
                        contentService
                                .publicNotices(schoolId, page, size)
                                .map(contentService::notice));
        String etag =
                "\""
                        + schoolId
                        + ":notices:"
                        + page
                        + ":"
                        + size
                        + ":"
                        + response.hashCode()
                        + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(response);
    }

    @GetMapping("/notices/{noticeId}")
    org.springframework.http.ResponseEntity<NoticeResponse> notice(
            @PathVariable UUID noticeId,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.NOTICES);
        NoticeResponse response =
                contentService.notice(contentService.publicNotice(schoolId, noticeId));
        String etag = "\"" + schoolId + ":notice:" + noticeId + ":" + response.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(response);
    }

    @GetMapping("/gallery/albums")
    org.springframework.http.ResponseEntity<PageResponse<GalleryAlbumResponse>> galleryAlbums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.GALLERY);
        PageResponse<GalleryAlbumResponse> response =
                PageResponse.from(contentService.publicAlbumPage(schoolId, page, size));
        String etag =
                "\"" + schoolId + ":albums:" + page + ":" + size + ":" + response.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(response);
    }

    @GetMapping("/gallery/albums/{albumId}/images")
    org.springframework.http.ResponseEntity<?> galleryImages(
            @PathVariable UUID albumId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.GALLERY);
        Object body;
        if (page != null || size != null) {
            int p = page != null ? page : 0;
            int s = size != null ? size : 20;
            body = PageResponse.from(contentService.publicAlbumImagesPage(schoolId, albumId, p, s));
        } else {
            body = contentService.publicAlbumImages(schoolId, albumId);
        }
        String etag =
                "\""
                        + schoolId
                        + ":album-images:"
                        + albumId
                        + ":"
                        + page
                        + ":"
                        + size
                        + ":"
                        + body.hashCode()
                        + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(body);
    }

    List<GalleryImageResponse> galleryImages(UUID albumId) {
        return contentService.publicAlbumImages(resolveTenantSchoolId(), albumId);
    }

    @GetMapping("/downloads")
    org.springframework.http.ResponseEntity<PageResponse<DownloadResponse>> downloads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            org.springframework.web.context.request.WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        featureAccessPolicy.requirePublicEnabled(schoolId, FeatureKey.DOWNLOADS);
        PageResponse<DownloadResponse> response =
                PageResponse.from(
                        contentService
                                .publicDownloads(schoolId, page, size)
                                .map(contentService::download));
        String etag =
                "\""
                        + schoolId
                        + ":downloads:"
                        + page
                        + ":"
                        + size
                        + ":"
                        + response.hashCode()
                        + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTENT_CACHE)
                .eTag(etag)
                .body(response);
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
