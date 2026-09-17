package com.rorfost.schoolportal.academic.application;

import com.rorfost.schoolportal.school.repository.SchoolRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class AcademicDefaultsInitializer {
  private final SchoolRepository schools;
  private final AcademicConfigurationService academics;

  AcademicDefaultsInitializer(SchoolRepository schools, AcademicConfigurationService academics) {
    this.schools = schools;
    this.academics = academics;
  }

  @EventListener(ApplicationReadyEvent.class)
  void ensurePrimaryStandardsExist() {
    // Startup seeding keeps first-time admin setup focused on subjects, not eight repeated entries.
    schools.findAll().forEach(school -> academics.ensureDefaultStandards(school.getId()));
  }
}
