package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.application.VisitService;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
class AdminVisitController {
    private final VisitService visits;
    private final SchoolRepository schools;

    AdminVisitController(VisitService visits, SchoolRepository schools) {
        this.visits = visits;
        this.schools = schools;
    }

    @PostMapping("/api/v1/admin/site-metrics/visits/reset")
    VisitResponse reset(@AuthenticationPrincipal PrincipalSession principal) {
        UUID schoolId = resolveAdminSchoolId(principal);
        return visits.reset(schoolId);
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
}
