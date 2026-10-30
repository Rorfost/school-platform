package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.application.VisitService;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/visits")
class VisitController {
    private final VisitService visits;
    private final SchoolRepository schools;

    VisitController(VisitService visits, SchoolRepository schools) {
        this.visits = visits;
        this.schools = schools;
    }

    @GetMapping
    VisitResponse get() {
        UUID schoolId = resolveTenantSchoolId();
        return visits.get(schoolId);
    }

    @PostMapping
    VisitResponse increment() {
        UUID schoolId = resolveTenantSchoolId();
        return visits.increment(schoolId);
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
