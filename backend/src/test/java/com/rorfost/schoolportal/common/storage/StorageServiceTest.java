package com.rorfost.schoolportal.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.config.StorageProperties;
import com.rorfost.schoolportal.common.config.UploadProperties;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Pattern;

class StorageServiceTest {
    private static final Pattern MATERIAL_PATH =
            Pattern.compile("materials/school-slug/[0-9a-f-]{36}\\.pdf");

    private final ObjectStorage objectStorage = Mockito.mock(ObjectStorage.class);

    @Test
    void uploadsPdfUnderTheSchoolSlugWithUuidFilenameAndOriginalFilenameMetadata()
            throws Exception {
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
    void createsAnOptimizedImageKitThumbnailUrl() {
        StorageService service = service(DataSize.ofMegabytes(10));

        assertThat(service.publicImageThumbnailUrl("gallery/school-slug/photo.jpg"))
                .isEqualTo(
                        "https://ik.imagekit.io/account/gallery/school-slug/photo.jpg?tr=w-800,h-600,c-at_max,q-80");
    }

    @Test
    void uploadsSignatureCheckedPngImagesForSchoolBranding() {
        StorageService service = service(DataSize.ofMegabytes(10));
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "school.png",
                        "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        StoredObject object = service.uploadPublicImage("branding/school-slug", file);

        assertThat(object.objectKey()).matches("branding/school-slug/[0-9a-f-]{36}\\.png");
        assertThat(object.contentType()).isEqualTo("image/png");
    }

    @Test
    void delegatesDeleteAndExistsToStorage() {
        StorageService service = service(DataSize.ofMegabytes(10));
        when(objectStorage.exists("imagekit", "downloads/school-slug/file.pdf")).thenReturn(true);

        service.delete("imagekit", "downloads/school-slug/file.pdf");

        assertThat(service.exists("imagekit", "downloads/school-slug/file.pdf")).isTrue();
        verify(objectStorage).delete("imagekit", "downloads/school-slug/file.pdf", null);
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
                new MockMultipartFile(
                        "file", "notes.pdf", "application/pdf", "not a PDF".getBytes());

        assertUploadFailure(
                () -> service.uploadPublicDocument("materials/school-slug", file),
                "upload_content_invalid");
    }

    @Test
    void rejectsAFileLargerThanTheConfiguredMaximum() {
        StorageService service = service(DataSize.ofBytes(5));
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "notes.pdf", "application/pdf", "%PDF-too-large".getBytes());

        assertUploadFailure(
                () -> service.uploadPublicDocument("materials/school-slug", file),
                "upload_too_large");
    }

    @Test
    void uploadsDocumentUnderCanonicalSchoolUuidPath() throws Exception {
        StorageService service = service(DataSize.ofMegabytes(10));
        UUID schoolId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file", "Lesson plan.pdf", "application/pdf", "%PDF-sample".getBytes());

        StoredObject object = service.uploadSchoolPublicDocument(schoolId, "materials", file);

        assertThat(object.objectKey())
                .matches(
                        "schools/11111111-1111-1111-1111-111111111111/materials/[0-9a-f-]{36}\\.pdf");
        assertThat(object.originalFilename()).isEqualTo("Lesson plan.pdf");
    }

    @Test
    void uploadsImageUnderCanonicalSchoolUuidPath() {
        StorageService service = service(DataSize.ofMegabytes(10));
        UUID schoolId = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "logo.png",
                        "image/png",
                        new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        StoredObject object = service.uploadSchoolPublicImage(schoolId, "branding", file);

        assertThat(object.objectKey())
                .matches(
                        "schools/22222222-2222-2222-2222-222222222222/branding/[0-9a-f-]{36}\\.png");
        assertThat(object.contentType()).isEqualTo("image/png");
    }

    @Test
    void assertKeyBelongsToSchoolAllowsMatchingCanonicalKey() {
        StorageService service = service(DataSize.ofMegabytes(10));
        UUID schoolId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        String key = "schools/11111111-1111-1111-1111-111111111111/materials/abc.pdf";

        // Should not throw
        service.assertKeyBelongsToSchool(key, schoolId);
    }

    @Test
    void assertKeyBelongsToSchoolRejectsKeyFromDifferentSchool() {
        StorageService service = service(DataSize.ofMegabytes(10));
        UUID schoolA = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID schoolB = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
        String schoolBKey =
                "schools/22222222-2222-2222-2222-222222222222/signatures/principal/abc.png";

        assertThatThrownBy(() -> service.assertKeyBelongsToSchool(schoolBKey, schoolA))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        exception ->
                                assertThat(((DomainException) exception).getCode())
                                        .isEqualTo("storage_key_ownership_violation"));
    }

    @Test
    void assertKeyBelongsToSchoolAllowsLegacyNonCanonicalKeys() {
        StorageService service = service(DataSize.ofMegabytes(10));
        UUID schoolId = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");

        // Old legacy slug paths must not throw, preserving backward compatibility for Dhadhana
        // files
        service.assertKeyBelongsToSchool("branding/dhadhana-school/old.png", schoolId);
        service.assertKeyBelongsToSchool("materials/dhadhana-school/lesson.pdf", schoolId);
        service.assertKeyBelongsToSchool(null, schoolId);
    }

    private StorageService service(DataSize maxFileSize) {
        return new StorageService(
                objectStorage,
                new StorageProperties("https://ik.imagekit.io/account/"),
                new UploadProperties(maxFileSize, DataSize.ofMegabytes(12)));
    }

    private void assertUploadFailure(Runnable action, String code) {
        assertThatThrownBy(() -> action.run())
                .isInstanceOf(DomainException.class)
                .satisfies(
                        exception ->
                                assertThat(((DomainException) exception).getCode())
                                        .isEqualTo(code));
    }
}
