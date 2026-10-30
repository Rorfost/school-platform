package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
class PublicAcademicController {
    private static final CacheControl ACADEMIC_SEMI_STATIC_CACHE =
            CacheControl.maxAge(Duration.ofSeconds(600))
                    .cachePublic()
                    .sMaxAge(Duration.ofSeconds(600));

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
    ResponseEntity<List<StandardResponse>> standards(WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        List<StandardResponse> list = service.standards(schoolId);
        String etag = "\"" + schoolId + ":" + list.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().cacheControl(ACADEMIC_SEMI_STATIC_CACHE).eTag(etag).body(list);
    }

    List<StandardResponse> standards() {
        return standards(null).getBody();
    }

    @GetMapping("/subjects")
    ResponseEntity<List<SubjectResponse>> subjects(WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        List<SubjectResponse> list = service.subjects(schoolId);
        String etag = "\"" + schoolId + ":" + list.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok().cacheControl(ACADEMIC_SEMI_STATIC_CACHE).eTag(etag).body(list);
    }

    List<SubjectResponse> subjects() {
        return subjects(null).getBody();
    }

    @GetMapping("/academic-years/current")
    ResponseEntity<AcademicYearResponse> currentYear(WebRequest webRequest) {
        UUID schoolId = resolveTenantSchoolId();
        AcademicYearResponse response =
                years.findBySchoolIdAndStatus(schoolId, AcademicYearStatus.CURRENT)
                        .map(AcademicYearResponse::from)
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "academic_year_not_found"));
        String etag = "\"" + schoolId + ":" + response.hashCode() + "\"";
        if (webRequest != null && webRequest.checkNotModified(etag)) {
            return null;
        }
        return ResponseEntity.ok()
                .cacheControl(ACADEMIC_SEMI_STATIC_CACHE)
                .eTag(etag)
                .body(response);
    }

    AcademicYearResponse currentYear() {
        return currentYear(null).getBody();
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
