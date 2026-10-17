package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.ExamResultService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ExamResultController {

    private final ExamResultService service;
    private final SchoolRepository schools;

    public ExamResultController(ExamResultService service, SchoolRepository schools) {
        this.service = service;
        this.schools = schools;
    }

    @GetMapping("/public/exam-results")
    public ResponseEntity<ExamResultResponse> getResult(
            @RequestParam("standard") String standard,
            @RequestParam(value = "class", required = false) String studentClass,
            @RequestParam("rollNumber") Integer rollNumber,
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {
        UUID schoolId = resolvePublicSchoolId();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.getResult(schoolId, standard, studentClass, rollNumber, resultType));
    }

    @PostMapping("/admin/exam-results/upload")
    public ResponseEntity<Map<String, String>> uploadResults(
            @AuthenticationPrincipal PrincipalSession principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "totalWorkingDays", required = false) String totalWorkingDays,
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {
        UUID schoolId = resolveAdminSchoolId(principal);
        service.processExcelUpload(schoolId, file, totalWorkingDays, resultType);
        return ResponseEntity.ok(Map.of("message", "Result uploaded successfully."));
    }

    @DeleteMapping("/admin/exam-results")
    public ResponseEntity<Map<String, String>> clearResults(
            @AuthenticationPrincipal PrincipalSession principal,
            @RequestParam(value = "resultType", defaultValue = ExamResultService.ANNUAL)
                    String resultType) {
        UUID schoolId = resolveAdminSchoolId(principal);
        service.clearResults(schoolId, resultType);
        return ResponseEntity.ok(Map.of("message", "Results cleared successfully."));
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
}
