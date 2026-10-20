package com.rorfost.schoolportal.content.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.content.application.ContentService;
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
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PublicContentTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";
    private static final String TOKEN_INACTIVE = "worker-secret-school-inactive";

    private NoticeRepository noticeRepository;
    private GalleryAlbumRepository galleryAlbumRepository;
    private GalleryImageRepository galleryImageRepository;
    private StudyMaterialRepository studyMaterialRepository;
    private DownloadRepository downloadRepository;
    private SchoolRepository schoolRepository;
    private StorageService storageService;
    private TenantResolver tenantResolver;
    private MockMvc mockMvc;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private School schoolA;
    private School schoolB;

    private UUID albumIdA;
    private UUID albumIdB;
    private UUID albumIdA_Draft;
    private GalleryAlbum albumA;
    private GalleryAlbum albumB;
    private GalleryAlbum albumADraft;

    private GalleryImage imageA1;
    private GalleryImage imageA2;
    private GalleryImage imageB1;

    private Notice noticeA;
    private Notice noticeB;

    private Download downloadA;
    private Download downloadB;

    private StudyMaterial materialA;
    private StudyMaterial materialB;

    @BeforeEach
    void setUp() {
        noticeRepository = mock(NoticeRepository.class);
        galleryAlbumRepository = mock(GalleryAlbumRepository.class);
        galleryImageRepository = mock(GalleryImageRepository.class);
        studyMaterialRepository = mock(StudyMaterialRepository.class);
        downloadRepository = mock(DownloadRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        storageService = mock(StorageService.class);
        tenantResolver = mock(TenantResolver.class);
        ObjectMapper objectMapper = new ObjectMapper();

        ContentService contentService =
                new ContentService(
                        storageService,
                        studyMaterialRepository,
                        noticeRepository,
                        galleryAlbumRepository,
                        galleryImageRepository,
                        downloadRepository,
                        schoolRepository,
                        mock(AcademicYearRepository.class),
                        mock(StandardSubjectRepository.class),
                        mock(StandardRepository.class),
                        mock(SubjectRepository.class),
                        mock(AuditLogService.class));

        // Default-enabled policy: no overrides stored, all core features enabled by default
        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolId(any())).thenReturn(java.util.Collections.emptyList());
        FeatureAccessPolicy featureAccessPolicy =
                new FeatureAccessPolicy(new FeatureFlagService(sfRepo));

        PublicContentController controller =
                new PublicContentController(contentService, schoolRepository, featureAccessPolicy);

        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();

        schoolIdA = UUID.randomUUID();
        schoolIdB = UUID.randomUUID();
        UUID schoolIdInactive = UUID.randomUUID();

        schoolA = new School("School A Primary", "school-a");
        schoolB = new School("School B Academy", "school-b");

        // Tenant resolution
        when(tenantResolver.resolveTenant(TOKEN_A))
                .thenReturn(Optional.of(new TenantContext(schoolIdA, "school-a")));
        when(tenantResolver.resolveTenant(TOKEN_B))
                .thenReturn(Optional.of(new TenantContext(schoolIdB, "school-b")));
        when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());

        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdA)).thenReturn(Optional.of(schoolA));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdB)).thenReturn(Optional.of(schoolB));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdInactive))
                .thenReturn(Optional.empty());

        // Storage mock
        when(storageService.publicUrl(any()))
                .thenAnswer(inv -> "https://ik.imagekit.io/" + inv.getArgument(0));
        when(storageService.publicImageThumbnailUrl(any()))
                .thenAnswer(inv -> "https://ik.imagekit.io/thumb/" + inv.getArgument(0));

        // Albums: duplicate-looking name across A and B
        albumIdA = UUID.randomUUID();
        albumA = new GalleryAlbum(schoolIdA, "વાર્ષિક રમતોત્સવ ૨૦૨૫", "School A sports album");
        albumA.publish(Instant.now().minus(2, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(schoolA, "id", schoolIdA);
        ReflectionTestUtils.setField(schoolB, "id", schoolIdB);
        ReflectionTestUtils.setField(albumA, "id", albumIdA);

        albumIdB = UUID.randomUUID();
        albumB = new GalleryAlbum(schoolIdB, "વાર્ષિક રમતોત્સવ ૨૦૨૫", "School B sports album");
        albumB.publish(Instant.now().minus(1, ChronoUnit.DAYS));
        ReflectionTestUtils.setField(albumB, "id", albumIdB);

        albumIdA_Draft = UUID.randomUUID();
        albumADraft = new GalleryAlbum(schoolIdA, "પ્રવેશોત્સવ ડ્રાફ્ટ", "Unpublished album");
        ReflectionTestUtils.setField(albumADraft, "id", albumIdA_Draft);

        // Album findByIdAndSchoolId
        when(galleryAlbumRepository.findByIdAndSchoolId(albumIdA, schoolIdA))
                .thenReturn(Optional.of(albumA));
        when(galleryAlbumRepository.findByIdAndSchoolId(albumIdA, schoolIdB))
                .thenReturn(Optional.empty());
        when(galleryAlbumRepository.findByIdAndSchoolId(albumIdB, schoolIdB))
                .thenReturn(Optional.of(albumB));
        when(galleryAlbumRepository.findByIdAndSchoolId(albumIdB, schoolIdA))
                .thenReturn(Optional.empty());
        when(galleryAlbumRepository.findByIdAndSchoolId(albumIdA_Draft, schoolIdA))
                .thenReturn(Optional.of(albumADraft));

        // Public albums list
        when(galleryAlbumRepository.findBySchoolIdAndStatus(
                        eq(schoolIdA), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(albumA)));
        when(galleryAlbumRepository.findBySchoolIdAndStatus(
                        eq(schoolIdB), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(albumB)));

        // Images for Album A
        imageA1 =
                new GalleryImage(
                        schoolIdA,
                        albumIdA,
                        "ik",
                        "gallery/school-a/img1.jpg",
                        "img1.jpg",
                        "image/jpeg",
                        1024,
                        null,
                        null,
                        "School A race",
                        "100m sprint",
                        1);
        imageA1.publish(Instant.now());

        imageA2 =
                new GalleryImage(
                        schoolIdA,
                        albumIdA,
                        "ik",
                        "gallery/school-a/img2.jpg",
                        "img2.jpg",
                        "image/jpeg",
                        2048,
                        null,
                        null,
                        "School A prize",
                        "Distribution",
                        2);
        imageA2.publish(Instant.now());

        // Images for Album B
        imageB1 =
                new GalleryImage(
                        schoolIdB,
                        albumIdB,
                        "ik",
                        "gallery/school-b/img1.jpg",
                        "img1.jpg",
                        "image/jpeg",
                        1024,
                        null,
                        null,
                        "School B relay",
                        "Relay team",
                        1);
        imageB1.publish(Instant.now());

        when(galleryImageRepository.findBySchoolIdAndGalleryAlbumIdAndStatusOrderBySortOrder(
                        eq(schoolIdA), eq(albumIdA), eq(PublicationStatus.PUBLISHED)))
                .thenReturn(List.of(imageA1, imageA2));
        when(galleryImageRepository.findBySchoolIdAndGalleryAlbumIdAndStatusOrderBySortOrder(
                        eq(schoolIdB), eq(albumIdB), eq(PublicationStatus.PUBLISHED)))
                .thenReturn(List.of(imageB1));
        when(galleryImageRepository.findBySchoolIdAndGalleryAlbumIdAndStatusOrderBySortOrder(
                        eq(schoolIdB), eq(albumIdA), eq(PublicationStatus.PUBLISHED)))
                .thenReturn(Collections.emptyList());

        // Image counts and batch summaries
        when(galleryImageRepository.countBySchoolIdAndGalleryAlbumIdAndStatus(
                        eq(schoolIdA), eq(albumIdA), eq(PublicationStatus.PUBLISHED)))
                .thenReturn(2L);
        when(galleryImageRepository.countBySchoolIdAndGalleryAlbumIdAndStatus(
                        eq(schoolIdB), eq(albumIdB), eq(PublicationStatus.PUBLISHED)))
                .thenReturn(1L);
        when(galleryImageRepository.findSummariesBySchoolIdAndAlbumIds(eq(schoolIdA), any()))
                .thenReturn(
                        List.of(
                                new com.rorfost.schoolportal.content.repository
                                        .GalleryAlbumSummaryRow(albumIdA, null, 2L)));
        when(galleryImageRepository.findSummariesBySchoolIdAndAlbumIds(eq(schoolIdB), any()))
                .thenReturn(
                        List.of(
                                new com.rorfost.schoolportal.content.repository
                                        .GalleryAlbumSummaryRow(albumIdB, null, 1L)));

        // Notices: duplicate title across A and B
        noticeA = new Notice(schoolIdA, "શાળા સમયમાં ફેરફાર", "School A: 8am to 1pm");
        noticeA.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        noticeB = new Notice(schoolIdB, "શાળા સમયમાં ફેરફાર", "School B: 9am to 2pm");
        noticeB.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        when(noticeRepository.findVisibleBySchoolIdAndStatus(
                        eq(schoolIdA), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(noticeA)));
        when(noticeRepository.findVisibleBySchoolIdAndStatus(
                        eq(schoolIdB), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(noticeB)));

        // Downloads: duplicate title across A and B
        downloadA =
                new Download(
                        schoolIdA,
                        UUID.randomUUID(),
                        "પ્રવેશ ફોર્મ ૨૦૨૫",
                        "Form description",
                        "General",
                        "ik",
                        "downloads/school-a/form.pdf",
                        "form.pdf",
                        "application/pdf",
                        4096,
                        "sha256-a",
                        "ik-file-a");
        downloadA.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        downloadB =
                new Download(
                        schoolIdB,
                        UUID.randomUUID(),
                        "પ્રવેશ ફોર્મ ૨૦૨૫",
                        "Form description",
                        "General",
                        "ik",
                        "downloads/school-b/form.pdf",
                        "form.pdf",
                        "application/pdf",
                        4096,
                        "sha256-b",
                        "ik-file-b");
        downloadB.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        when(downloadRepository.findBySchoolIdAndStatus(
                        eq(schoolIdA), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(downloadA)));
        when(downloadRepository.findBySchoolIdAndStatus(
                        eq(schoolIdB), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(downloadB)));

        // Study materials: duplicate title across A and B
        materialA =
                new StudyMaterial(
                        schoolIdA,
                        null,
                        null,
                        "ગણિત પ્રકરણ ૧",
                        "Notes A",
                        "PDF",
                        "ik",
                        "materials/school-a/math.pdf",
                        "math.pdf",
                        "application/pdf",
                        8192,
                        null,
                        null);
        materialA.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        materialB =
                new StudyMaterial(
                        schoolIdB,
                        null,
                        null,
                        "ગણિત પ્રકરણ ૧",
                        "Notes B",
                        "PDF",
                        "ik",
                        "materials/school-b/math.pdf",
                        "math.pdf",
                        "application/pdf",
                        8192,
                        null,
                        null);
        materialB.publish(Instant.now().minus(1, ChronoUnit.DAYS));

        when(studyMaterialRepository.findBySchoolIdAndStatus(
                        eq(schoolIdA), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(materialA)));
        when(studyMaterialRepository.findBySchoolIdAndStatus(
                        eq(schoolIdB), eq(PublicationStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(materialB)));
    }

    @Test
    @DisplayName("GET /gallery/albums returns only requesting tenant's albums")
    void publicAlbums_returnsOnlyRequestTenantAlbums() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/gallery/albums")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("School A sports album"))
                .andExpect(jsonPath("$.items[0].imageCount").value(2));

        mockMvc.perform(
                        get("/api/v1/public/gallery/albums")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("School B sports album"))
                .andExpect(jsonPath("$.items[0].imageCount").value(1));
    }

    @Test
    @DisplayName("GET /gallery/albums/{albumId}/images returns only requesting tenant's photos")
    void publicAlbumImages_returnsOnlyRequestTenantPhotos() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/gallery/albums/" + albumIdA + "/images")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].altText").value("School A race"))
                .andExpect(jsonPath("$[1].altText").value("School A prize"));

        mockMvc.perform(
                        get("/api/v1/public/gallery/albums/" + albumIdB + "/images")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].altText").value("School B relay"));
    }

    @Test
    @DisplayName(
            "Cross-tenant access: Token B requesting School A's album ID returns 404 without leaking existence")
    void crossTenant_albumAccess_returns404() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/gallery/albums/" + albumIdA + "/images")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("gallery_album_not_found"));
    }

    @Test
    @DisplayName(
            "Cross-tenant access: Token A requesting School B's album ID returns 404 without leaking existence")
    void crossTenant_albumAccess_reverse_returns404() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/gallery/albums/" + albumIdB + "/images")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("gallery_album_not_found"));
    }

    @Test
    @DisplayName("Requesting unpublished/draft album returns 404")
    void unpublishedAlbum_returns404() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/gallery/albums/" + albumIdA_Draft + "/images")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("gallery_album_not_found"));
    }

    @Test
    @DisplayName("GET /notices returns only requesting tenant's visible notices")
    void publicNotices_returnsOnlyRequestTenantNotices() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("School A: 8am to 1pm"));

        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("School B: 9am to 2pm"));
    }

    @Test
    @DisplayName("GET /downloads returns only requesting tenant's published downloads")
    void publicDownloads_returnsOnlyRequestTenantDownloads() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/downloads")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].filename").value("form.pdf"))
                .andExpect(
                        jsonPath("$.items[0].url")
                                .value("https://ik.imagekit.io/downloads/school-a/form.pdf"));

        mockMvc.perform(
                        get("/api/v1/public/downloads")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].filename").value("form.pdf"))
                .andExpect(
                        jsonPath("$.items[0].url")
                                .value("https://ik.imagekit.io/downloads/school-b/form.pdf"));
    }

    @Test
    @DisplayName("GET /materials returns only requesting tenant's published study materials")
    void publicMaterials_returnsOnlyRequestTenantMaterials() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/materials")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Notes A"))
                .andExpect(
                        jsonPath("$.items[0].url")
                                .value("https://ik.imagekit.io/materials/school-a/math.pdf"));

        mockMvc.perform(
                        get("/api/v1/public/materials")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Notes B"))
                .andExpect(
                        jsonPath("$.items[0].url")
                                .value("https://ik.imagekit.io/materials/school-b/math.pdf"));
    }

    @Test
    @DisplayName("Client spoofed headers and params are strictly ignored")
    void spoofedHeaders_ignored_strictlyTokenDerived() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B)
                                .header("X-Portal-School-Slug", "school-a")
                                .param("schoolId", schoolIdA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("School B: 9am to 2pm"));
    }

    @Test
    @DisplayName("Missing or invalid gateway tokens are rejected with 403 Forbidden")
    void missingOrInvalidToken_rejectedWith403() throws Exception {
        mockMvc.perform(get("/api/v1/public/notices"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));

        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_INACTIVE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }
}
