package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.AssessmentService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AssessmentController {
  private final AssessmentService service;

  AssessmentController(AssessmentService service) {
    this.service = service;
  }

  @GetMapping("/api/v1/admin/assessment-types")
  List<AssessmentTypeResponse> types() {
    return service.types();
  }

  @GetMapping("/api/v1/admin/assessments")
  List<AssessmentResponse> list(@AuthenticationPrincipal PrincipalSession principal) {
    return service.list(principal.schoolId());
  }

  @PostMapping("/api/v1/admin/assessments")
  AssessmentResponse create(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody AssessmentRequest request) {
    return service.create(principal.schoolId(), principal.adminUserId(), request);
  }

  @PutMapping("/api/v1/admin/assessments/{id}")
  AssessmentResponse update(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody AssessmentRequest request) {
    return service.update(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @PostMapping("/api/v1/admin/assessments/{id}/publish")
  AssessmentResponse publish(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.publish(principal.schoolId(), principal.adminUserId(), id);
  }

  @PostMapping("/api/v1/admin/assessments/{id}/archive")
  AssessmentResponse archive(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.archive(principal.schoolId(), principal.adminUserId(), id);
  }

  @GetMapping("/api/v1/public/assessment-types")
  List<AssessmentTypeResponse> publicTypes() {
    return service.types();
  }
}
