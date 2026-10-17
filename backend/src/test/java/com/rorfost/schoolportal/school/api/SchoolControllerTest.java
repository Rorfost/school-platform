package com.rorfost.schoolportal.school.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.application.SchoolService;
import com.rorfost.schoolportal.school.domain.AdminRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

class SchoolControllerTest {

    private SchoolService schoolService;
    private SchoolController controller;

    private UUID schoolId;
    private UUID adminUserId;
    private PrincipalSession principal;

    @BeforeEach
    void setUp() {
        schoolService = Mockito.mock(SchoolService.class);
        controller = new SchoolController(schoolService);

        schoolId = UUID.randomUUID();
        adminUserId = UUID.randomUUID();
        principal =
                new PrincipalSession(
                        adminUserId, schoolId, "principal@example.com", AdminRole.PRINCIPAL, false);
    }

    @Test
    void publicSchool_shouldReturnPublicSchoolData() {
        SchoolResponse mockSchool =
                new SchoolResponse(
                        schoolId,
                        "શ્રી ધાણધાર પ્રાથમિક શાળા",
                        "ધાણધાર શાળા",
                        "24020100101",
                        "મુ. ધાણધાર",
                        "ધાણધાર",
                        "સમી",
                        "પાટણ",
                        "ગુજરાત",
                        "384245",
                        "school@example.com",
                        "9876543210",
                        "https://dhadhana.example.com",
                        null,
                        "શાળા પરિચય",
                        "1985",
                        "ગુજરાતી",
                        "પ્રાથમિક શાળા",
                        "https://ik.imagekit.io/logo.png",
                        "https://ik.imagekit.io/seal.png");

        when(schoolService.getPublic()).thenReturn(mockSchool);

        SchoolResponse response = controller.publicSchool();

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("શ્રી ધાણધાર પ્રાથમિક શાળા");
        assertThat(response.schoolCode()).isEqualTo("24020100101");
    }

    @Test
    void publicProfile_shouldReturnPublicPrincipalProfile() {
        PrincipalProfileResponse mockProfile =
                new PrincipalProfileResponse(
                        "રમેશભાઈ એમ. પટેલ",
                        "આચાર્યશ્રી પરિચય",
                        "M.A., B.Ed.",
                        "આચાર્ય",
                        "શાળા પરિવાર તરફથી સંદેશ",
                        "portrait-key",
                        "sig-key",
                        "https://ik.imagekit.io/sig.png",
                        "principal@example.com",
                        "9876543210",
                        true,
                        true);

        when(schoolService.getPublicProfile()).thenReturn(mockProfile);

        PrincipalProfileResponse response = controller.publicProfile();

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("રમેશભાઈ એમ. પટેલ");
        assertThat(response.designation()).isEqualTo("આચાર્ય");
    }

    @Test
    void school_shouldReturnSchoolForAuthenticatedPrincipal() {
        SchoolResponse mockSchool =
                new SchoolResponse(
                        schoolId,
                        "ટેસ્ટ પ્રાથમિક શાળા",
                        "ટેસ્ટ શાળા",
                        "12345",
                        "સરનામું",
                        "શહેર",
                        "તાલુકો",
                        "જિલ્લો",
                        "ગુજરાત",
                        "384001",
                        "admin@example.com",
                        "1234567890",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

        when(schoolService.get(schoolId)).thenReturn(mockSchool);

        SchoolResponse response = controller.school(principal);

        assertThat(response.name()).isEqualTo("ટેસ્ટ પ્રાથમિક શાળા");
        verify(schoolService).get(schoolId);
    }

    @Test
    void replaceLogo_andRemoveLogo_shouldDelegateToService() {
        MockMultipartFile file =
                new MockMultipartFile("file", "logo.png", "image/png", new byte[] {1, 2, 3});

        controller.replaceLogo(principal, file);
        verify(schoolService).replaceLogo(schoolId, adminUserId, file);

        controller.removeLogo(principal);
        verify(schoolService).removeLogo(schoolId, adminUserId);
    }

    @Test
    void replaceSeal_andRemoveSeal_shouldDelegateToService() {
        MockMultipartFile file =
                new MockMultipartFile("file", "seal.png", "image/png", new byte[] {4, 5, 6});

        controller.replaceSeal(principal, file);
        verify(schoolService).replaceSeal(schoolId, adminUserId, file);

        controller.removeSeal(principal);
        verify(schoolService).removeSeal(schoolId, adminUserId);
    }

    @Test
    void replaceSignature_andDeleteSignature_shouldDelegateToService() {
        MockMultipartFile file =
                new MockMultipartFile("file", "sig.png", "image/png", new byte[] {7, 8, 9});

        controller.replacePrincipalSignature(principal, file);
        verify(schoolService).replacePrincipalSignature(schoolId, adminUserId, file);

        controller.deletePrincipalSignature(principal);
        verify(schoolService).deletePrincipalSignature(schoolId, adminUserId);
    }
}
