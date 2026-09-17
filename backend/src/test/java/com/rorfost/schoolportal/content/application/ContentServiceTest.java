package com.rorfost.schoolportal.content.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.content.api.GalleryImageMetadataRequest;
import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import com.rorfost.schoolportal.content.domain.GalleryImage;
import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.repository.DownloadRepository;
import com.rorfost.schoolportal.content.repository.GalleryAlbumRepository;
import com.rorfost.schoolportal.content.repository.GalleryImageRepository;
import com.rorfost.schoolportal.content.repository.NoticeRepository;
import com.rorfost.schoolportal.content.repository.StudyMaterialRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ContentServiceTest {
  private final UUID schoolId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final UUID albumId = UUID.randomUUID();
  private final StorageService storage = Mockito.mock(StorageService.class);
  private final GalleryAlbumRepository albums = Mockito.mock(GalleryAlbumRepository.class);
  private final GalleryImageRepository images = Mockito.mock(GalleryImageRepository.class);
  private final SchoolRepository schools = Mockito.mock(SchoolRepository.class);
  private final List<GalleryImage> albumImages = new ArrayList<>();
  private ContentService service;
  private GalleryAlbum album;

  @BeforeEach
  void setUp() {
    service =
        new ContentService(
            storage,
            Mockito.mock(StudyMaterialRepository.class),
            Mockito.mock(NoticeRepository.class),
            albums,
            images,
            Mockito.mock(DownloadRepository.class),
            schools,
            Mockito.mock(AcademicYearRepository.class),
            Mockito.mock(StandardSubjectRepository.class),
            Mockito.mock(StandardRepository.class),
            Mockito.mock(SubjectRepository.class),
            Mockito.mock(AuditLogService.class));
    album = album();

    when(albums.findByIdAndSchoolIdForUpdate(albumId, schoolId)).thenReturn(Optional.of(album));
    when(albums.findByIdAndSchoolId(albumId, schoolId)).thenReturn(Optional.of(album));
    when(images.findByGalleryAlbumIdOrderBySortOrder(albumId))
        .thenAnswer(
            invocation ->
                albumImages.stream()
                    .sorted(Comparator.comparingInt(GalleryImage::getSortOrder))
                    .toList());
    when(images.findByIdAndGalleryAlbumId(any(UUID.class), any(UUID.class)))
        .thenAnswer(
            invocation ->
                albumImages.stream()
                    .filter(image -> image.getId().equals(invocation.getArgument(0)))
                    .findFirst());
    when(schools.findById(schoolId)).thenReturn(Optional.of(new School("School", "school-slug")));
    when(storage.publicUrl(anyString()))
        .thenAnswer(invocation -> "https://assets/" + invocation.getArgument(0));
    when(storage.publicImageThumbnailUrl(anyString()))
        .thenAnswer(invocation -> "https://assets/thumb/" + invocation.getArgument(0));
  }

  @Test
  void publishingAlbumPublishesItsExistingImagesAndUnpublishingHidesThem() {
    GalleryImage first = addImage(1);
    GalleryImage second = addImage(2);

    service.publishAlbum(schoolId, actorId, albumId);

    assertThat(album.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    assertThat(first.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    assertThat(second.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
    assertThat(service.publicAlbumImages(schoolId, albumId)).hasSize(2);

    service.unpublishAlbum(schoolId, actorId, albumId);

    assertThat(album.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    assertThat(first.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    assertThat(second.getStatus()).isEqualTo(PublicationStatus.DRAFT);
    assertThatThrownBy(() -> service.publicAlbumImages(schoolId, albumId))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode())
                    .isEqualTo("gallery_album_not_found"));
  }

  @Test
  void uploadsUseAutomaticOrderSetTheFirstCoverAndPublishIntoPublishedAlbums() {
    album.publish(java.time.Instant.now());
    AtomicInteger uploads = new AtomicInteger();
    when(images.maxSortOrderByGalleryAlbumId(albumId)).thenReturn(0);
    when(storage.uploadPublicImage(anyString(), any()))
        .thenAnswer(invocation -> storedObject(uploads.incrementAndGet()));
    when(images.saveAndFlush(any(GalleryImage.class)))
        .thenAnswer(
            invocation -> {
              GalleryImage image = invocation.getArgument(0);
              setId(image, UUID.randomUUID());
              albumImages.add(image);
              return image;
            });

    service.uploadImages(
        schoolId,
        actorId,
        albumId,
        List.of("First", "Second", "Third"),
        java.util.Arrays.asList(null, "Caption", null),
        List.of(imageFile("one.jpg"), imageFile("two.jpg"), imageFile("three.jpg")));

    assertThat(albumImages).extracting(GalleryImage::getSortOrder).containsExactly(1, 2, 3);
    assertThat(album.getCoverImageId()).isEqualTo(albumImages.get(0).getId());
    assertThat(albumImages)
        .allSatisfy(image -> assertThat(image.getStatus()).isEqualTo(PublicationStatus.PUBLISHED));
  }

  @Test
  void reorderRequiresTheCompleteDistinctAlbumImageSetAndNormalizesTheOrder() {
    GalleryImage first = addImage(1);
    GalleryImage second = addImage(2);
    GalleryImage third = addImage(3);

    service.reorderImages(
        schoolId, actorId, albumId, List.of(third.getId(), first.getId(), second.getId()));

    assertThat(third.getSortOrder()).isEqualTo(1);
    assertThat(first.getSortOrder()).isEqualTo(2);
    assertThat(second.getSortOrder()).isEqualTo(3);
    verify(images, Mockito.times(2)).flush();

    assertThatThrownBy(
            () ->
                service.reorderImages(
                    schoolId,
                    actorId,
                    albumId,
                    List.of(first.getId(), first.getId(), third.getId())))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode())
                    .isEqualTo("gallery_image_ids_duplicate"));
    assertThatThrownBy(
            () ->
                service.reorderImages(
                    schoolId,
                    actorId,
                    albumId,
                    List.of(first.getId(), second.getId(), UUID.randomUUID())))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode())
                    .isEqualTo("gallery_image_ids_invalid"));
  }

  @Test
  void deletingImagesReplacesTheCoverNormalizesOrderAndClearsTheFinalCover() {
    GalleryImage first = addImage(1);
    GalleryImage second = addImage(2);
    album.setCoverImageId(first.getId());
    doAnswer(invocation -> albumImages.remove(invocation.getArgument(0)))
        .when(images)
        .delete(any(GalleryImage.class));

    service.deleteImage(schoolId, actorId, albumId, first.getId());

    assertThat(album.getCoverImageId()).isEqualTo(second.getId());
    assertThat(second.getSortOrder()).isEqualTo(1);
    verify(storage).delete(first.getStorageBucket(), first.getObjectKey());

    service.deleteImage(schoolId, actorId, albumId, second.getId());

    assertThat(album.getCoverImageId()).isNull();
  }

  @Test
  void metadataAndCoverUpdatesAreScopedToTheRequestedAlbum() {
    GalleryImage first = addImage(1);
    GalleryImage second = addImage(2);

    service.setAlbumCover(schoolId, actorId, albumId, second.getId());
    service.updateImageMetadata(
        schoolId,
        actorId,
        albumId,
        first.getId(),
        new GalleryImageMetadataRequest("Updated", "Caption"));

    assertThat(album.getCoverImageId()).isEqualTo(second.getId());
    assertThat(first.getAltText()).isEqualTo("Updated");
    assertThat(first.getCaption()).isEqualTo("Caption");
    assertThatThrownBy(() -> service.setAlbumCover(schoolId, actorId, albumId, UUID.randomUUID()))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode())
                    .isEqualTo("gallery_image_not_found"));
  }

  @Test
  void storageFailuresPreventGalleryMetadataDeletion() {
    GalleryImage image = addImage(1);
    doThrow(new IllegalStateException("ImageKit unavailable"))
        .when(storage)
        .delete(image.getStorageBucket(), image.getObjectKey());

    assertThatThrownBy(() -> service.deleteImage(schoolId, actorId, albumId, image.getId()))
        .isInstanceOf(IllegalStateException.class);

    verify(images, never()).delete(image);
  }

  @Test
  void failedImageMetadataPersistenceCleansUpTheNewlyUploadedStorageObject() {
    StoredObject object = storedObject(1);
    when(images.maxSortOrderByGalleryAlbumId(albumId)).thenReturn(0);
    when(storage.uploadPublicImage(anyString(), any())).thenReturn(object);
    when(images.saveAndFlush(any(GalleryImage.class)))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThatThrownBy(
            () ->
                service.uploadImage(
                    schoolId, actorId, albumId, "Photo", null, imageFile("photo.jpg")))
        .isInstanceOf(IllegalStateException.class);

    verify(storage).delete(object);
  }

  private GalleryAlbum album() {
    GalleryAlbum item = new GalleryAlbum(schoolId, "Sports day", null);
    setId(item, albumId);
    return item;
  }

  private GalleryImage addImage(int order) {
    GalleryImage image =
        new GalleryImage(
            schoolId,
            albumId,
            "imagekit",
            "gallery/school-slug/" + UUID.randomUUID() + ".jpg",
            "photo.jpg",
            "image/jpeg",
            10,
            "a".repeat(64),
            "Photo " + order,
            null,
            order);
    setId(image, UUID.randomUUID());
    albumImages.add(image);
    return image;
  }

  private StoredObject storedObject(int index) {
    return new StoredObject(
        "imagekit",
        "gallery/school-slug/" + index + ".jpg",
        "photo.jpg",
        "image/jpeg",
        10,
        "a".repeat(64));
  }

  private MockMultipartFile imageFile(String filename) {
    return new MockMultipartFile(
        "file", filename, "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff});
  }

  private void setId(Object entity, UUID id) {
    ReflectionTestUtils.setField(entity, "id", id);
  }
}
