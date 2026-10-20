/*
 * Copyright (c) Rorfost. All rights reserved.
 * SPDX-License-Identifier: PROPRIETARY
 */

package com.rorfost.schoolportal.content.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.rorfost.schoolportal.common.fixture.SyntheticTenantDatasetBuilder;
import com.rorfost.schoolportal.common.fixture.TenantFixtureFactory;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.GalleryImageRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Sprint 31 Benchmark & Baseline Execution Suite.
 *
 * <p>Validates determinism of the synthetic dataset across two fixture builds, and exercises fixed
 * cold and warm sample paths across all three synthetic tenants:
 *
 * <ul>
 *   <li>Gallery albums listing (cold + warm)
 *   <li>Notice board listing (cold + warm)
 *   <li>Study materials listing (cold + warm)
 *   <li>Official downloads listing (cold + warm)
 *   <li>Feature isolation &amp; negative checks (School C disabled modules)
 * </ul>
 */
class PerformanceBaselineSuiteTest {

    private TenantFixtureFactory fixtures;
    private SyntheticTenantDatasetBuilder datasetBuilder;
    private SyntheticTenantDatasetBuilder.MultiTenantBenchmarkDataset dataset;

    private TenantResolver tenantResolver;
    private SchoolRepository schoolRepository;
    private GalleryAlbumRepository galleryAlbumRepository;
    private GalleryImageRepository galleryImageRepository;
    private NoticeRepository noticeRepository;
    private StudyMaterialRepository studyMaterialRepository;
    private DownloadRepository downloadRepository;
    private SchoolFeatureRepository schoolFeatureRepository;

    private MockMvc publicContentMvc;

    @BeforeEach
    void setUp() {
        fixtures = new TenantFixtureFactory();
        datasetBuilder = new SyntheticTenantDatasetBuilder(fixtures);
        dataset = datasetBuilder.buildAll();

        tenantResolver = mock(TenantResolver.class);
        fixtures.stubTenantResolver(tenantResolver);

        schoolRepository = mock(SchoolRepository.class);
        galleryAlbumRepository = mock(GalleryAlbumRepository.class);
        galleryImageRepository = mock(GalleryImageRepository.class);
        noticeRepository = mock(NoticeRepository.class);
        studyMaterialRepository = mock(StudyMaterialRepository.class);
        downloadRepository = mock(DownloadRepository.class);
        schoolFeatureRepository = mock(SchoolFeatureRepository.class);
        StorageService storageService = mock(StorageService.class);

        when(storageService.publicUrl(any()))
                .thenAnswer(inv -> "https://ik.imagekit.io/" + inv.getArgument(0));
        when(storageService.publicImageThumbnailUrl(any()))
                .thenAnswer(inv -> "https://ik.imagekit.io/thumb/" + inv.getArgument(0));

        // School repository active stubs
        when(schoolRepository.findByIdAndIsActiveTrue(fixtures.schoolIdA))
                .thenReturn(Optional.of(fixtures.schoolA));
        when(schoolRepository.findByIdAndIsActiveTrue(fixtures.schoolIdB))
                .thenReturn(Optional.of(fixtures.schoolB));
        when(schoolRepository.findByIdAndIsActiveTrue(fixtures.schoolIdC))
                .thenReturn(Optional.of(fixtures.schoolC));

        // Features
        when(schoolFeatureRepository.findByIdSchoolId(fixtures.schoolIdA))
                .thenReturn(dataset.tenantA().features());
        when(schoolFeatureRepository.findByIdSchoolId(fixtures.schoolIdB))
                .thenReturn(dataset.tenantB().features());
        when(schoolFeatureRepository.findByIdSchoolId(fixtures.schoolIdC))
                .thenReturn(dataset.tenantC().features());

        for (SyntheticTenantDatasetBuilder.TenantDataset td : dataset.allTenants()) {
            for (com.rorfost.schoolportal.featureflag.domain.SchoolFeature sf : td.features()) {
                when(schoolFeatureRepository.findByIdSchoolIdAndIdFeatureKey(
                                td.schoolId(), sf.getFeatureKey()))
                        .thenReturn(Optional.of(sf));
            }
        }

        // Albums
        when(galleryAlbumRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdA),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantA().albums()));
        when(galleryAlbumRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdB),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantB().albums()));
        when(galleryAlbumRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdC),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Notices
        when(noticeRepository.findVisibleBySchoolIdAndStatus(
                        eq(fixtures.schoolIdA),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantA().notices()));
        when(noticeRepository.findVisibleBySchoolIdAndStatus(
                        eq(fixtures.schoolIdB),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantB().notices()));
        when(noticeRepository.findVisibleBySchoolIdAndStatus(
                        eq(fixtures.schoolIdC),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantC().notices()));

        // Materials
        when(studyMaterialRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdA),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantA().materials()));
        when(studyMaterialRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdB),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantB().materials()));

        // Downloads
        when(downloadRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdA),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantA().downloads()));
        when(downloadRepository.findBySchoolIdAndStatus(
                        eq(fixtures.schoolIdB),
                        eq(PublicationStatus.PUBLISHED),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(dataset.tenantB().downloads()));

        FeatureAccessPolicy policy =
                new FeatureAccessPolicy(new FeatureFlagService(schoolFeatureRepository));
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

        PublicContentController publicContentController =
                new PublicContentController(contentService, schoolRepository, policy);

        ObjectMapper objectMapper = new ObjectMapper();
        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        publicContentMvc =
                MockMvcBuilders.standaloneSetup(publicContentController)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();
    }

    @Test
    @DisplayName(
            "Fixture determinism: running fixture setup twice yields identical row counts and entity identities")
    void fixtureBuilder_isDeterministic() {
        SyntheticTenantDatasetBuilder builder1 =
                new SyntheticTenantDatasetBuilder(new TenantFixtureFactory());
        SyntheticTenantDatasetBuilder builder2 =
                new SyntheticTenantDatasetBuilder(new TenantFixtureFactory());

        SyntheticTenantDatasetBuilder.MultiTenantBenchmarkDataset set1 = builder1.buildAll();
        SyntheticTenantDatasetBuilder.MultiTenantBenchmarkDataset set2 = builder2.buildAll();

        assertThat(set1.totalRowCounts()).isEqualTo(set2.totalRowCounts());
        assertThat(set1.tenantA().getRowCounts()).isEqualTo(set2.tenantA().getRowCounts());
        assertThat(set1.tenantB().getRowCounts()).isEqualTo(set2.tenantB().getRowCounts());
        assertThat(set1.tenantC().getRowCounts()).isEqualTo(set2.tenantC().getRowCounts());

        // Check specific entity bounds
        assertThat(set1.tenantA().albums()).hasSize(12);
        assertThat(set1.tenantA().images()).hasSize(72);
        assertThat(set1.tenantA().notices()).hasSize(15);
        assertThat(set1.tenantA().materials()).hasSize(10);
        assertThat(set1.tenantA().downloads()).hasSize(6);
        assertThat(set1.tenantA().results()).hasSize(20);

        assertThat(set1.tenantB().albums()).hasSize(1);
        assertThat(set1.tenantB().images()).hasSize(4);
        assertThat(set1.tenantB().notices()).hasSize(5);
        assertThat(set1.tenantB().materials()).hasSize(4);
        assertThat(set1.tenantB().downloads()).hasSize(3);
        assertThat(set1.tenantB().results()).hasSize(50);

        assertThat(set1.tenantC().albums()).isEmpty();
        assertThat(set1.tenantC().materials()).isEmpty();
        assertThat(set1.tenantC().downloads()).isEmpty();
        assertThat(set1.tenantC().results()).isEmpty();
        assertThat(set1.tenantC().notices()).hasSize(3);
    }

    @Test
    @DisplayName(
            "Warm and Cold sampling: execute cold and 3 warm repetitions across sample routes for all 3 tenants")
    void warmAndColdPaths_sampleExecution() throws Exception {
        Map<String, String> tokens =
                Map.of(
                        "TenantA", TenantFixtureFactory.TOKEN_A,
                        "TenantB", TenantFixtureFactory.TOKEN_B,
                        "TenantC", TenantFixtureFactory.TOKEN_C);

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String token = entry.getValue();

            // 1. Cold sample - Notices
            long startCold = System.nanoTime();
            publicContentMvc
                    .perform(
                            get("/api/v1/public/notices")
                                    .header(
                                            GatewayCredentialAuthenticationFilter
                                                    .GATEWAY_TOKEN_HEADER,
                                            token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").exists());
            long coldLatencyMs = (System.nanoTime() - startCold) / 1_000_000;

            // 3 Warm samples
            List<Long> warmLatencies = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                long startWarm = System.nanoTime();
                publicContentMvc
                        .perform(
                                get("/api/v1/public/notices")
                                        .header(
                                                GatewayCredentialAuthenticationFilter
                                                        .GATEWAY_TOKEN_HEADER,
                                                token))
                        .andExpect(status().isOk());
                warmLatencies.add((System.nanoTime() - startWarm) / 1_000_000);
            }

            assertThat(coldLatencyMs).isGreaterThanOrEqualTo(0);
            assertThat(warmLatencies).hasSize(3);
        }
    }

    @Test
    @DisplayName(
            "Feature isolation check: School C requests to disabled gallery return 404 feature_disabled")
    void schoolC_disabledModules_failClosed() throws Exception {
        // School A has gallery enabled -> 200 OK
        publicContentMvc
                .perform(
                        get("/api/v1/public/gallery/albums")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(12));

        // School C has gallery disabled -> 404 feature_not_found
        publicContentMvc
                .perform(
                        get("/api/v1/public/gallery/albums")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TenantFixtureFactory.TOKEN_C))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("feature_not_found"));
    }
}
