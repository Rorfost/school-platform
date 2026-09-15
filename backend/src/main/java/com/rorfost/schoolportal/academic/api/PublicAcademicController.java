package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
class PublicAcademicController {
  private final AcademicConfigurationService service;
  private final SchoolRepository schools;
  private final AcademicYearRepository years;

  PublicAcademicController(
      AcademicConfigurationService service,
      SchoolRepository schools,
      AcademicYearRepository years) {
    this.service = service;
    this.schools = schools;
    this.years = years;
  }

  @GetMapping("/standards")
  List<StandardResponse> standards() {
    return service.standards(schoolId());
  }

  @GetMapping("/subjects")
  List<SubjectResponse> subjects() {
    return service.subjects(schoolId());
  }

  @GetMapping("/academic-years/current")
  AcademicYearResponse currentYear() {
    return years
        .findBySchoolIdAndStatus(schoolId(), AcademicYearStatus.CURRENT)
        .map(AcademicYearResponse::from)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "academic_year_not_found"));
  }

  private java.util.UUID schoolId() {
    return schools
        .findFirstByIsActiveTrueOrderByCreatedAtAsc()
        .map(value -> value.getId())
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_configured"));
  }
}
