package com.rorfost.schoolportal.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.auth.application.AdminBootstrapService;
import com.rorfost.schoolportal.common.config.AdminBootstrapProperties;
import com.rorfost.schoolportal.school.domain.AdminUser;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.AdminUserRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

class AdminBootstrapServiceTest {

    private final AdminUserRepository adminUserRepository = Mockito.mock(AdminUserRepository.class);
    private final SchoolRepository schoolRepository = Mockito.mock(SchoolRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsOneMandatoryChangePrincipalFromConfiguredInputs() {
        AdminBootstrapProperties properties =
                new AdminBootstrapProperties(
                        "Principal@Example.Test", "InitialPassword12", "school");
        School school = new School("School", "school");
        when(schoolRepository.findBySlug("school")).thenReturn(Optional.of(school));
        when(adminUserRepository.findBySchoolIdAndEmail(school.getId(), "principal@example.test"))
                .thenReturn(Optional.empty());
        AdminBootstrapService service =
                new AdminBootstrapService(
                        properties, adminUserRepository, schoolRepository, passwordEncoder);

        service.initialize();

        ArgumentCaptor<AdminUser> user = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository).save(user.capture());
        assertThat(user.getValue().getEmail()).isEqualTo("principal@example.test");
        assertThat(user.getValue().getSchoolId()).isEqualTo(school.getId());
        assertThat(user.getValue().isMustChangePassword()).isTrue();
        assertThat(passwordEncoder.matches("InitialPassword12", user.getValue().getPasswordHash()))
                .isTrue();
    }

    @Test
    void neverOverwritesAnExistingAdministratorForSameSchool() {
        AdminBootstrapProperties properties =
                new AdminBootstrapProperties(
                        "principal@example.test", "InitialPassword12", "school");
        School school = new School("School", "school");
        AdminUser existing =
                new AdminUser(school.getId(), "principal@example.test", "existingHash");
        when(schoolRepository.findBySlug("school")).thenReturn(Optional.of(school));
        when(adminUserRepository.findBySchoolIdAndEmail(school.getId(), "principal@example.test"))
                .thenReturn(Optional.of(existing));

        AdminBootstrapService service =
                new AdminBootstrapService(
                        properties, adminUserRepository, schoolRepository, passwordEncoder);

        service.initialize();

        verify(adminUserRepository, never()).save(any());
    }
}
