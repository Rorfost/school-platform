package com.rorfost.schoolportal.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.api.PageResponse;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PublicContentControllerTest {

    private ContentService contentService;
    private SchoolRepository schoolRepository;
    private PublicContentController controller;
    private FeatureAccessPolicy featureAccessPolicy;

    private UUID schoolId;
    private School school;

    @BeforeEach
    void setUp() {
        contentService = Mockito.mock(ContentService.class);
        schoolRepository = Mockito.mock(SchoolRepository.class);
        // Use a real FeatureAccessPolicy backed by a no-override repository so defaults apply
        SchoolFeatureRepository sfRepo = Mockito.mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolId(Mockito.any())).thenReturn(java.util.Collections.emptyList());
        featureAccessPolicy = new FeatureAccessPolicy(new FeatureFlagService(sfRepo));
        controller =
                new PublicContentController(contentService, schoolRepository, featureAccessPolicy);

        schoolId = UUID.randomUUID();
        school = Mockito.mock(School.class);
        when(school.getId()).thenReturn(schoolId);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindTenant(UUID id, String slug) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TenantContextHolder.setTenantContext(request, new TenantContext(id, slug));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void notices_shouldReturnPublicNoticesPageForVerifiedTenant() {
        bindTenant(schoolId, "dhadhana-school");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        Notice notice = new Notice(schoolId, "નવું પરિપત્ર", "મહત્વપૂર્ણ સૂચના");
        notice.publish(Instant.now());

        NoticeResponse responseDto =
                new NoticeResponse(
                        UUID.randomUUID(),
                        "નવું પરિપત્ર",
                        "મહત્વપૂર્ણ સૂચના",
                        false,
                        null,
                        "circular.pdf",
                        "https://ik.imagekit.io/circular.pdf",
                        "PUBLISHED");

        when(contentService.publicNotices(schoolId, 0, 20))
                .thenReturn(new PageImpl<>(List.of(notice), PageRequest.of(0, 20), 1));
        when(contentService.notice(notice)).thenReturn(responseDto);

        PageResponse<NoticeResponse> result = controller.notices(0, 20, null).getBody();

        assertThat(result).isNotNull();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("નવું પરિપત્ર");
        verify(contentService).publicNotices(schoolId, 0, 20);
    }

    @Test
    void notice_shouldReturnOnlyTheRequestedTenantVisibleNotice() {
        bindTenant(schoolId, "dhadhana-school");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        UUID noticeId = UUID.randomUUID();
        Notice notice = new Notice(schoolId, "શાળા સમય", "School A notice body");
        NoticeResponse responseDto =
                new NoticeResponse(
                        noticeId,
                        "શાળા સમય",
                        "School A notice body",
                        false,
                        null,
                        null,
                        null,
                        "PUBLISHED");
        when(contentService.publicNotice(schoolId, noticeId)).thenReturn(notice);
        when(contentService.notice(notice)).thenReturn(responseDto);

        NoticeResponse response = controller.notice(noticeId, null).getBody();

        assertThat(response).isEqualTo(responseDto);
        verify(contentService).publicNotice(schoolId, noticeId);
    }

    @Test
    void galleryImages_shouldReturnImagesForAlbumForVerifiedTenant() {
        bindTenant(schoolId, "dhadhana-school");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        UUID albumId = UUID.randomUUID();
        GalleryImageResponse img =
                new GalleryImageResponse(
                        UUID.randomUUID(),
                        "વાર્ષિક ઉત્સવ ફોટો",
                        "મુખ્ય કાર્યક્રમ",
                        1,
                        "https://ik.imagekit.io/photo.jpg",
                        "https://ik.imagekit.io/photo-thumb.jpg",
                        "PUBLISHED");

        when(contentService.publicAlbumImages(schoolId, albumId)).thenReturn(List.of(img));

        List<GalleryImageResponse> images = controller.galleryImages(albumId);

        assertThat(images).hasSize(1);
        assertThat(images.get(0).altText()).isEqualTo("વાર્ષિક ઉત્સવ ફોટો");
        verify(contentService).publicAlbumImages(schoolId, albumId);
    }

    @Test
    void publicEndpoints_shouldThrowWhenTenantMissing() {
        RequestContextHolder.resetRequestAttributes();

        assertThatThrownBy(() -> controller.notices(0, 20, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "school_not_found");
    }

    @Test
    void publicEndpoints_shouldThrowWhenSchoolInactiveOrNotFound() {
        bindTenant(schoolId, "dhadhana-school");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.notices(0, 20, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "school_not_found");
    }

    @Test
    void publicEndpoints_disabledFeature_shouldReturn404() {
        // Use a FeatureAccessPolicy backed by a repository that returns a disabled override
        SchoolFeatureRepository sfRepo = Mockito.mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolIdAndIdFeatureKey(schoolId, FeatureKey.NOTICES.name()))
                .thenReturn(
                        Optional.of(
                                new com.rorfost.schoolportal.featureflag.domain.SchoolFeature(
                                        schoolId, FeatureKey.NOTICES, false)));
        when(sfRepo.findByIdSchoolId(schoolId))
                .thenReturn(
                        java.util.List.of(
                                new com.rorfost.schoolportal.featureflag.domain.SchoolFeature(
                                        schoolId, FeatureKey.NOTICES, false)));
        FeatureAccessPolicy disabledPolicy =
                new FeatureAccessPolicy(new FeatureFlagService(sfRepo));
        PublicContentController disabledController =
                new PublicContentController(contentService, schoolRepository, disabledPolicy);

        bindTenant(schoolId, "dhadhana-school");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        assertThatThrownBy(() -> disabledController.notices(0, 20, null))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }
}
