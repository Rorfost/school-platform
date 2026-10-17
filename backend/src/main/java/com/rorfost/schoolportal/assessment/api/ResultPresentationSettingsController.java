package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.assessment.application.ResultPresentationSettingsService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResultPresentationSettingsController {
    private final ResultPresentationSettingsService service;
    private final SchoolRepository schools;
    private final PrincipalProfileRepository principalProfiles;
    private final StandardRepository standards;
    private final StorageService storage;

    public ResultPresentationSettingsController(
            ResultPresentationSettingsService service,
            SchoolRepository schools,
            PrincipalProfileRepository principalProfiles,
            StandardRepository standards,
            StorageService storage) {
        this.service = service;
        this.schools = schools;
        this.principalProfiles = principalProfiles;
        this.standards = standards;
        this.storage = storage;
    }

    @GetMapping("/public/result-settings")
    ResultPresentationSettingsResponse publicSettings(
            @RequestParam(required = false) String standard) {
        UUID schoolId = resolvePublicSchoolId();
        String principalSignatureUrl =
                principalProfiles
                        .findBySchoolId(schoolId)
                        .map(PrincipalProfile::getSignatureObjectKey)
                        .filter(Objects::nonNull)
                        .map(storage::publicUrl)
                        .orElse(null);
        Optional<Standard> matchingStandard =
                standard == null
                        ? Optional.empty()
                        : standards
                                .findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId)
                                .stream()
                                .filter(value -> matchesStandard(value, standard.trim()))
                                .findFirst();
        return service.get(schoolId)
                .withSignatures(
                        principalSignatureUrl,
                        matchingStandard.map(Standard::getClassTeacherName).orElse(null),
                        matchingStandard
                                .map(Standard::getClassTeacherSignatureObjectKey)
                                .filter(Objects::nonNull)
                                .map(storage::publicUrl)
                                .orElse(null));
    }

    @GetMapping("/admin/result-settings")
    ResultPresentationSettingsResponse adminSettings(
            @AuthenticationPrincipal PrincipalSession principal) {
        UUID schoolId = resolveAdminSchoolId(principal);
        return service.get(schoolId);
    }

    @PutMapping("/admin/result-settings")
    ResultPresentationSettingsResponse update(
            @AuthenticationPrincipal PrincipalSession principal,
            @Valid @RequestBody ResultPresentationSettingsRequest request) {
        UUID schoolId = resolveAdminSchoolId(principal);
        return service.update(schoolId, request);
    }

    private UUID resolvePublicSchoolId() {
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

    private UUID resolveAdminSchoolId(PrincipalSession principal) {
        if (principal == null || principal.schoolId() == null) {
            throw new DomainException(HttpStatus.UNAUTHORIZED, "authentication_required");
        }
        TenantContext tenant =
                TenantContextHolder.getCurrentTenant()
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.FORBIDDEN, "missing_tenant_context"));
        if (!principal.schoolId().equals(tenant.schoolId())) {
            throw new DomainException(HttpStatus.FORBIDDEN, "cross_tenant_access_denied");
        }
        return schools.findByIdAndIsActiveTrue(tenant.schoolId())
                .map(School::getId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
    }

    private boolean matchesStandard(Standard candidate, String requested) {
        if (requested.equalsIgnoreCase(candidate.getDisplayName())
                || requested.equalsIgnoreCase(candidate.getCode())) return true;
        String requestedNumber = requested.replaceAll("[^0-9]", "");
        String displayNumber = candidate.getDisplayName().replaceAll("[^0-9]", "");
        return !requestedNumber.isBlank() && requestedNumber.equals(displayNumber);
    }
}
