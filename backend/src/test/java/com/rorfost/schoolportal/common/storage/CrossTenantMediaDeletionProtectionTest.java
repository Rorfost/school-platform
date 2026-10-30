package com.rorfost.schoolportal.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.application.SchoolService;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CrossTenantMediaDeletionProtectionTest {

    private final UUID schoolA = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID schoolB = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID actorA = UUID.randomUUID();

    @Mock private StorageService storageService;
    @Mock private SchoolRepository schoolRepository;
    @Mock private PrincipalProfileRepository principalProfileRepository;
    @Mock private StandardRepository standardRepository;
    @Mock private AuditLogService auditLogService;

    private SchoolService schoolService;
    private AcademicConfigurationService academicConfigurationService;

    @BeforeEach
    void setUp() {
        schoolService =
                new SchoolService(
                        schoolRepository,
                        principalProfileRepository,
                        auditLogService,
                        storageService);
        academicConfigurationService =
                new AcademicConfigurationService(
                        Mockito.mock(
                                com.rorfost.schoolportal.academic.repository.AcademicYearRepository
                                        .class),
                        standardRepository,
                        Mockito.mock(
                                com.rorfost.schoolportal.academic.repository.SubjectRepository
                                        .class),
                        Mockito.mock(
                                com.rorfost.schoolportal.academic.repository
                                        .StandardSubjectRepository.class),
                        Mockito.mock(
                                com.rorfost.schoolportal.academic.repository.StandardClassRepository
                                        .class),
                        auditLogService,
                        storageService);
    }

    @Test
    @DisplayName(
            "Principal of School A cannot delete School B's branding logo even if stored in School A's DB entity")
    void schoolACannotDeleteSchoolBLogo() {
        School schoolEntityA = new School("School A", "school-a");
        String foreignKey = "schools/" + schoolB + "/branding/foreign-logo.png";
        schoolEntityA.changeLogo(foreignKey);

        when(schoolRepository.findById(schoolA)).thenReturn(Optional.of(schoolEntityA));
        org.mockito.Mockito.doThrow(
                        new DomainException(
                                HttpStatus.FORBIDDEN, "storage_key_ownership_violation"))
                .when(storageService)
                .assertKeyBelongsToSchool(foreignKey, schoolA);

        schoolService.removeLogo(schoolA, actorA);

        // Verification: DB updated to null logo, but remote delete was blocked by
        // assertKeyBelongsToSchool
        assertThat(schoolEntityA.getLogoObjectKey()).isNull();
        verify(storageService).assertKeyBelongsToSchool(foreignKey, schoolA);
        verify(storageService, Mockito.never()).deletePublicObject(foreignKey);
    }

    @Test
    @DisplayName("Principal of School A cannot delete School B's principal signature")
    void schoolACannotDeleteSchoolBSignature() {
        PrincipalProfile profileA = new PrincipalProfile(schoolA, "Principal A");
        String foreignKey = "schools/" + schoolB + "/signatures/principal/foreign-sig.png";
        profileA.changeSignature(foreignKey);

        when(principalProfileRepository.findBySchoolId(schoolA)).thenReturn(Optional.of(profileA));
        org.mockito.Mockito.doThrow(
                        new DomainException(
                                HttpStatus.FORBIDDEN, "storage_key_ownership_violation"))
                .when(storageService)
                .assertKeyBelongsToSchool(foreignKey, schoolA);

        schoolService.deletePrincipalSignature(schoolA, actorA);

        assertThat(profileA.getSignatureObjectKey()).isNull();
        verify(storageService).assertKeyBelongsToSchool(foreignKey, schoolA);
        verify(storageService, Mockito.never()).deletePublicObject(foreignKey);
    }

    @Test
    @DisplayName("School A cannot delete School B's class teacher signature")
    void schoolACannotDeleteSchoolBClassTeacherSignature() {
        UUID standardId = UUID.randomUUID();
        Standard standard = new Standard(schoolA, "STD-10", "10-A", (short) 1);
        String foreignKey =
                "schools/" + schoolB + "/signatures/class-teacher/foreign-teacher-sig.png";
        standard.changeClassTeacherSignature(foreignKey);

        when(standardRepository.findByIdAndSchoolId(standardId, schoolA))
                .thenReturn(Optional.of(standard));
        org.mockito.Mockito.doThrow(
                        new DomainException(
                                HttpStatus.FORBIDDEN, "storage_key_ownership_violation"))
                .when(storageService)
                .assertKeyBelongsToSchool(foreignKey, schoolA);

        academicConfigurationService.deleteClassTeacherSignature(schoolA, actorA, standardId);

        assertThat(standard.getClassTeacherSignatureObjectKey()).isNull();
        verify(storageService).assertKeyBelongsToSchool(foreignKey, schoolA);
        verify(storageService, Mockito.never()).deletePublicObject(foreignKey);
    }

    @Test
    @DisplayName(
            "Direct call to assertKeyBelongsToSchool rejects cross-tenant canonical object key")
    void assertKeyBelongsToSchoolDirectRejection() {
        StorageService liveStorage =
                new StorageService(
                        Mockito.mock(ObjectStorage.class),
                        new com.rorfost.schoolportal.common.config.StorageProperties(
                                "https://ik.imagekit.io/test/"),
                        new com.rorfost.schoolportal.common.config.UploadProperties(
                                org.springframework.util.unit.DataSize.ofMegabytes(10),
                                org.springframework.util.unit.DataSize.ofMegabytes(12)));

        String foreignKey = "schools/" + schoolB + "/branding/foreign-logo.png";

        assertThatThrownBy(() -> liveStorage.assertKeyBelongsToSchool(foreignKey, schoolA))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                            assertThat(de.getCode()).isEqualTo("storage_key_ownership_violation");
                        });
    }
}
