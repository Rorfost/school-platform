package com.rorfost.schoolportal.assessment.api;

<<<<<<< HEAD
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.assessment.application.ResultPresentationSettingsService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
=======
import com.rorfost.schoolportal.assessment.application.ResultPresentationSettingsService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.exception.DomainException;
>>>>>>> baaa4954004ce590e678a54c0862bfd396a37505
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
<<<<<<< HEAD
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
      @org.springframework.web.bind.annotation.RequestParam(required = false) String standard) {
    java.util.UUID schoolId =
        schools
            .findFirstByIsActiveTrueOrderByCreatedAtAsc()
            .map(value -> value.getId())
            .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_configured"));
    String principalSignatureUrl =
        principalProfiles
            .findBySchoolId(schoolId)
            .map(value -> value.getSignatureObjectKey())
            .filter(java.util.Objects::nonNull)
            .map(storage::publicUrl)
            .orElse(null);
    var matchingStandard =
        standard == null
            ? java.util.Optional.<com.rorfost.schoolportal.academic.domain.Standard>empty()
            : standards.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId).stream()
                .filter(value -> matchesStandard(value, standard.trim()))
                .findFirst();
    return service
        .get(schoolId)
        .withSignatures(
            principalSignatureUrl,
            matchingStandard.map(value -> value.getClassTeacherName()).orElse(null),
            matchingStandard
                .map(value -> value.getClassTeacherSignatureObjectKey())
                .filter(java.util.Objects::nonNull)
                .map(storage::publicUrl)
                .orElse(null));
=======

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
>>>>>>> baaa4954004ce590e678a54c0862bfd396a37505
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
<<<<<<< HEAD

  private boolean matchesStandard(
      com.rorfost.schoolportal.academic.domain.Standard candidate, String requested) {
    if (requested.equalsIgnoreCase(candidate.getDisplayName())
        || requested.equalsIgnoreCase(candidate.getCode())) return true;
    String requestedNumber = requested.replaceAll("[^0-9]", "");
    String displayNumber = candidate.getDisplayName().replaceAll("[^0-9]", "");
    return !requestedNumber.isBlank() && requestedNumber.equals(displayNumber);
  }
=======
>>>>>>> baaa4954004ce590e678a54c0862bfd396a37505
}
