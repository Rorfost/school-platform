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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminBootstrapServiceTest {

  private final AdminUserRepository adminUserRepository = Mockito.mock(AdminUserRepository.class);
  private final SchoolRepository schoolRepository = Mockito.mock(SchoolRepository.class);
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void createsOneMandatoryChangePrincipalFromConfiguredInputs() {
    AdminBootstrapProperties properties =
        new AdminBootstrapProperties("Principal@Example.Test", "InitialPassword12", "school");
    School school = new School("School", "school");
    when(adminUserRepository.count()).thenReturn(0L);
    when(schoolRepository.findBySlug("school")).thenReturn(Optional.of(school));
    AdminBootstrapService service =
        new AdminBootstrapService(
            properties, adminUserRepository, schoolRepository, passwordEncoder);

    service.initialize();

    ArgumentCaptor<AdminUser> user = ArgumentCaptor.forClass(AdminUser.class);
    verify(adminUserRepository).save(user.capture());
    assertThat(user.getValue().getEmail()).isEqualTo("principal@example.test");
    assertThat(user.getValue().isMustChangePassword()).isTrue();
    assertThat(passwordEncoder.matches("InitialPassword12", user.getValue().getPasswordHash()))
        .isTrue();
  }

  @Test
  void neverOverwritesAnExistingAdministrator() {
    AdminBootstrapProperties properties =
        new AdminBootstrapProperties("principal@example.test", "InitialPassword12", "school");
    when(adminUserRepository.count()).thenReturn(1L);
    AdminBootstrapService service =
        new AdminBootstrapService(
            properties, adminUserRepository, schoolRepository, passwordEncoder);

    service.initialize();

    verify(adminUserRepository, never()).save(any());
    verify(schoolRepository, never()).findBySlug(any());
  }
}
