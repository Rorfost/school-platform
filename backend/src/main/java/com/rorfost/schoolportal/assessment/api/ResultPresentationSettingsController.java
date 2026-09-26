package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.application.ResultPresentationSettingsService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ResultPresentationSettingsController {
  private final ResultPresentationSettingsService service;
  private final SchoolRepository schools;

  public ResultPresentationSettingsController(
      ResultPresentationSettingsService service, SchoolRepository schools) {
    this.service = service;
    this.schools = schools;
  }

  @GetMapping("/public/result-settings")
  ResultPresentationSettingsResponse publicSettings() {
    return service.get(
        schools
            .findFirstByIsActiveTrueOrderByCreatedAtAsc()
            .map(value -> value.getId())
            .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_configured")));
  }

  @GetMapping("/admin/result-settings")
  ResultPresentationSettingsResponse adminSettings(
      @AuthenticationPrincipal PrincipalSession principal) {
    return service.get(principal.schoolId());
  }

  @PutMapping("/admin/result-settings")
  ResultPresentationSettingsResponse update(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody ResultPresentationSettingsRequest request) {
    return service.update(principal.schoolId(), request);
  }
}
