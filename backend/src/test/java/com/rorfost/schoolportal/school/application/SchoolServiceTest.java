package com.rorfost.schoolportal.school.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

class SchoolServiceTest {
    private final SchoolRepository schools = Mockito.mock(SchoolRepository.class);
    private final StorageService storage = Mockito.mock(StorageService.class);
    private final SchoolService service =
            new SchoolService(
                    schools,
                    Mockito.mock(PrincipalProfileRepository.class),
                    Mockito.mock(AuditLogService.class),
                    storage);

    @Test
    void replacesThePersistedLogoAndCleansUpThePreviousObject() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("Example School", "example-school");
        school.changeLogo("branding/example-school/old.jpg");
        StoredObject uploaded =
                new StoredObject(
                        "imagekit",
                        "branding/example-school/new.jpg",
                        "new.jpg",
                        "image/jpeg",
                        3,
                        "checksum",
                        "file-id");
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));
        when(storage.uploadPublicImage(eq("branding/example-school"), any())).thenReturn(uploaded);
        when(storage.publicUrl(uploaded.objectKey())).thenReturn("https://assets.example/new.jpg");

        var response =
                service.replaceLogo(
                        schoolId,
                        UUID.randomUUID(),
                        new MockMultipartFile(
                                "file", "new.jpg", "image/jpeg", new byte[] {1, 2, 3}));

        assertThat(school.getLogoObjectKey()).isEqualTo(uploaded.objectKey());
        assertThat(response.logoUrl()).isEqualTo("https://assets.example/new.jpg");
        verify(schools).saveAndFlush(school);
        verify(storage).deletePublicObject("branding/example-school/old.jpg");
    }

    @Test
    void removesThePersistedLogoOnlyAfterSavingTheEmptyReference() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("Example School", "example-school");
        school.changeLogo("branding/example-school/old.jpg");
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));

        service.removeLogo(schoolId, UUID.randomUUID());

        assertThat(school.getLogoObjectKey()).isNull();
        verify(schools).saveAndFlush(school);
        verify(storage).deletePublicObject("branding/example-school/old.jpg");
    }

    @Test
    void replacesThePersistedSealAndCleansUpThePreviousObject() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("Example School", "example-school");
        school.changeSeal("branding/example-school/seal/old.png");
        StoredObject uploaded =
                new StoredObject(
                        "imagekit",
                        "branding/example-school/seal/new.png",
                        "new.png",
                        "image/png",
                        3,
                        "checksum",
                        "file-id");
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));
        when(storage.uploadPublicImage(eq("branding/example-school"), any())).thenReturn(uploaded);
        when(storage.publicUrl(uploaded.objectKey()))
                .thenReturn("https://assets.example/seal/new.png");

        var response =
                service.replaceSeal(
                        schoolId,
                        UUID.randomUUID(),
                        new MockMultipartFile(
                                "file", "new.png", "image/png", new byte[] {1, 2, 3}));

        assertThat(school.getSealObjectKey()).isEqualTo(uploaded.objectKey());
        assertThat(response.sealUrl()).isEqualTo("https://assets.example/seal/new.png");
        verify(schools).saveAndFlush(school);
        verify(storage).deletePublicObject("branding/example-school/seal/old.png");
    }

    @Test
    void removesThePersistedSealOnlyAfterSavingTheEmptyReference() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("Example School", "example-school");
        school.changeSeal("branding/example-school/seal/old.png");
        when(schools.findById(schoolId)).thenReturn(Optional.of(school));

        service.removeSeal(schoolId, UUID.randomUUID());

        assertThat(school.getSealObjectKey()).isNull();
        verify(schools).saveAndFlush(school);
        verify(storage).deletePublicObject("branding/example-school/seal/old.png");
    }
}
