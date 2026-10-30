package com.rorfost.schoolportal.school.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.school.api.PrincipalProfileResponse;
import com.rorfost.schoolportal.school.api.SchoolResponse;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

class SchoolServiceTest {
    private final SchoolRepository schools = Mockito.mock(SchoolRepository.class);
    private final PrincipalProfileRepository profiles =
            Mockito.mock(PrincipalProfileRepository.class);
    private final StorageService storage = Mockito.mock(StorageService.class);
    private final SchoolService service =
            new SchoolService(schools, profiles, Mockito.mock(AuditLogService.class), storage);

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
        when(storage.uploadSchoolPublicImage(eq(schoolId), eq("branding"), any()))
                .thenReturn(uploaded);
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
        verify(storage).assertKeyBelongsToSchool("branding/example-school/old.jpg", schoolId);
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
        verify(storage).assertKeyBelongsToSchool("branding/example-school/old.jpg", schoolId);
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
        when(storage.uploadSchoolPublicImage(eq(schoolId), eq("branding"), any()))
                .thenReturn(uploaded);
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
        verify(storage).assertKeyBelongsToSchool("branding/example-school/seal/old.png", schoolId);
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
        verify(storage).assertKeyBelongsToSchool("branding/example-school/seal/old.png", schoolId);
        verify(storage).deletePublicObject("branding/example-school/seal/old.png");
    }

    @Test
    void getPublic_shouldReturnSchoolWhenActive() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("શ્રી પ્રાથમિક શાળા", "primary-school");
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        SchoolResponse response = service.getPublic(schoolId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(school.getId());
        assertThat(response.name()).isEqualTo("શ્રી પ્રાથમિક શાળા");
        verify(schools).findByIdAndIsActiveTrue(schoolId);
    }

    @Test
    void getPublic_shouldThrowNotFoundWhenSchoolNotFoundOrInactive() {
        UUID schoolId = UUID.randomUUID();
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublic(schoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "school_not_found");
    }

    @Test
    void getPublicProfile_shouldReturnPublicProfileWhenSchoolActiveAndProfilePublic() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("School A", "school-a");
        PrincipalProfile profile = new PrincipalProfile(schoolId, "ડૉ. પટેલ");
        profile.update(
                "ડૉ. પટેલ",
                "બાયોગ્રાફી",
                "M.Sc., B.Ed.",
                "આચાર્યશ્રી",
                "સંદેશ",
                "portrait-key",
                "principal@example.com",
                "9876543210",
                true,
                true);

        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));
        when(profiles.findBySchoolId(schoolId)).thenReturn(Optional.of(profile));

        PrincipalProfileResponse response = service.getPublicProfile(schoolId);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("ડૉ. પટેલ");
        assertThat(response.designation()).isEqualTo("આચાર્યશ્રી");
        assertThat(response.email()).isEqualTo("principal@example.com");
    }

    @Test
    void getPublicProfile_shouldThrowNotFoundWhenSchoolNotFoundOrInactive() {
        UUID schoolId = UUID.randomUUID();
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(schoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "school_not_found");

        verify(profiles, never()).findBySchoolId(any());
    }

    @Test
    void getPublicProfile_shouldThrowNotFoundWhenProfileMissing() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("School B", "school-b");
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));
        when(profiles.findBySchoolId(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPublicProfile(schoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "principal_profile_not_found");
    }

    @Test
    void getPublicProfile_shouldThrowNotFoundWhenProfileIsNotPublic() {
        UUID schoolId = UUID.randomUUID();
        School school = new School("School B", "school-b");
        PrincipalProfile profile = new PrincipalProfile(schoolId, "ખાનગી આચાર્ય");
        profile.update("ખાનગી આચાર્ય", null, null, "આચાર્ય", null, null, null, null, false, false);

        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));
        when(profiles.findBySchoolId(schoolId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.getPublicProfile(schoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "principal_profile_not_found");
    }
}
