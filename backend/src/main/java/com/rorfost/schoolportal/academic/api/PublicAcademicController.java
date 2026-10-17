package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
        return service.standards(resolveTenantSchoolId());
    }

    @GetMapping("/subjects")
    List<SubjectResponse> subjects() {
        return service.subjects(resolveTenantSchoolId());
    }

    @GetMapping("/academic-years/current")
    AcademicYearResponse currentYear() {
        return years.findBySchoolIdAndStatus(resolveTenantSchoolId(), AcademicYearStatus.CURRENT)
                .map(AcademicYearResponse::from)
                .orElseThrow(
                        () -> new DomainException(HttpStatus.NOT_FOUND, "academic_year_not_found"));
    }

    private UUID resolveTenantSchoolId() {
        UUID schoolId =
                TenantContextHolder.getCurrentTenant()
                        .map(TenantContext::schoolId)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "school_not_found"));
        return schools.findByIdAndIsActiveTrue(schoolId)
                .map(School::getId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
    }
}
