package com.rorfost.schoolportal.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.config.StorageProperties;
import com.rorfost.schoolportal.common.config.UploadProperties;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.io.InputStream;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class StorageServiceTest {
  private static final Pattern MATERIAL_PATH =
      Pattern.compile("materials/school-slug/[0-9a-f-]{36}\\.pdf");

  private final ObjectStorage objectStorage = Mockito.mock(ObjectStorage.class);

  @Test
  void uploadsPdfUnderTheSchoolSlugWithUuidFilenameAndOriginalFilenameMetadata() throws Exception {
    StorageService service = service(DataSize.ofMegabytes(10));
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "Lesson plan.pdf", "application/pdf", "%PDF-sample".getBytes());

    StoredObject object = service.uploadPublicDocument("materials/school-slug", file);

    ArgumentCaptor<String> objectKey = ArgumentCaptor.forClass(String.class);
    verify(objectStorage)
        .put(
            org.mockito.ArgumentMatchers.eq("imagekit"),
            objectKey.capture(),
            any(InputStream.class),
            org.mockito.ArgumentMatchers.eq(11L),
            org.mockito.ArgumentMatchers.eq("application/pdf"));
    assertThat(objectKey.getValue()).matches(MATERIAL_PATH);
    assertThat(object.objectKey()).isEqualTo(objectKey.getValue());
    assertThat(object.originalFilename()).isEqualTo("Lesson plan.pdf");
  }

  @Test
  void createsPublicUrlFromImageKitEndpointAndStoredPath() {
    StorageService service = service(DataSize.ofMegabytes(10));

    assertThat(service.publicUrl("materials/school-slug/abc.pdf"))
        .isEqualTo("https://ik.imagekit.io/account/materials/school-slug/abc.pdf");
  }

  @Test
  void delegatesDeleteAndExistsToStorage() {
    StorageService service = service(DataSize.ofMegabytes(10));
    when(objectStorage.exists("imagekit", "downloads/school-slug/file.pdf")).thenReturn(true);

    service.delete("imagekit", "downloads/school-slug/file.pdf");

    assertThat(service.exists("imagekit", "downloads/school-slug/file.pdf")).isTrue();
    verify(objectStorage).delete("imagekit", "downloads/school-slug/file.pdf");
  }

  @Test
  void rejectsUnsupportedMimeType() {
    StorageService service = service(DataSize.ofMegabytes(10));
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", "text/plain", "plain text".getBytes());

    assertUploadFailure(
        () -> service.uploadPublicDocument("materials/school-slug", file),
        "upload_type_unsupported");
  }

  @Test
  void rejectsAnInvalidFileSignature() {
    StorageService service = service(DataSize.ofMegabytes(10));
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.pdf", "application/pdf", "not a PDF".getBytes());

    assertUploadFailure(
        () -> service.uploadPublicDocument("materials/school-slug", file),
        "upload_content_invalid");
  }

  @Test
  void rejectsAFileLargerThanTheConfiguredMaximum() {
    StorageService service = service(DataSize.ofBytes(5));
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.pdf", "application/pdf", "%PDF-too-large".getBytes());

    assertUploadFailure(
        () -> service.uploadPublicDocument("materials/school-slug", file), "upload_too_large");
  }

  private StorageService service(DataSize maxFileSize) {
    return new StorageService(
        objectStorage,
        new StorageProperties("https://ik.imagekit.io/account/"),
        new UploadProperties(maxFileSize, DataSize.ofMegabytes(12)));
  }

  private void assertUploadFailure(Runnable action, String code) {
    assertThatThrownBy(action)
        .isInstanceOf(DomainException.class)
        .satisfies(
            exception -> assertThat(((DomainException) exception).getCode()).isEqualTo(code));
  }
}
