package com.rorfost.schoolportal.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.api.PageResponse;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.content.domain.Notice;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PublicContentControllerTest {

    private ContentService contentService;
    private SchoolRepository schoolRepository;
    private PublicContentController controller;

    private UUID schoolId;
    private School school;

    @BeforeEach
    void setUp() {
        contentService = Mockito.mock(ContentService.class);
        schoolRepository = Mockito.mock(SchoolRepository.class);
        controller = new PublicContentController(contentService, schoolRepository);

        schoolId = UUID.randomUUID();
        school = Mockito.mock(School.class);
        when(school.getId()).thenReturn(schoolId);
    }

    @Test
    void notices_shouldReturnPublicNoticesPage() {
        when(schoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(school));

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

        PageResponse<NoticeResponse> result = controller.notices(0, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("નવું પરિપત્ર");
        verify(contentService).publicNotices(schoolId, 0, 20);
    }

    @Test
    void galleryImages_shouldReturnImagesForAlbum() {
        when(schoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.of(school));

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
    }

    @Test
    void publicEndpoints_shouldThrowWhenSchoolNotConfigured() {
        when(schoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc())
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.notices(0, 20))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        error ->
                                assertThat(((DomainException) error).getCode())
                                        .isEqualTo("school_not_configured"));
    }
}
