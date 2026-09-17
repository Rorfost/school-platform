package com.rorfost.schoolportal.school.api;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.school.application.SchoolService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
class SchoolController {
  private final SchoolService schoolService;

  SchoolController(SchoolService schoolService) {
    this.schoolService = schoolService;
  }

  @GetMapping("/api/v1/public/school")
  SchoolResponse publicSchool() {
    return schoolService.getPublic();
  }

  @GetMapping("/api/v1/public/principal-profile")
  PrincipalProfileResponse publicProfile() {
    return schoolService.getPublicProfile();
  }

  @GetMapping("/api/v1/admin/school")
  SchoolResponse school(@AuthenticationPrincipal PrincipalSession principal) {
    return schoolService.get(principal.schoolId());
  }

  @PutMapping("/api/v1/admin/school")
  SchoolResponse updateSchool(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody SchoolUpdateRequest request) {
    return schoolService.update(principal.schoolId(), principal.adminUserId(), request);
  }

  @PostMapping(value = "/api/v1/admin/school/logo", consumes = "multipart/form-data")
  SchoolResponse replaceLogo(
      @AuthenticationPrincipal PrincipalSession principal, @RequestParam MultipartFile file) {
    return schoolService.replaceLogo(principal.schoolId(), principal.adminUserId(), file);
  }

  @DeleteMapping("/api/v1/admin/school/logo")
  SchoolResponse removeLogo(@AuthenticationPrincipal PrincipalSession principal) {
    return schoolService.removeLogo(principal.schoolId(), principal.adminUserId());
  }

  @GetMapping("/api/v1/admin/principal-profile")
  PrincipalProfileResponse profile(@AuthenticationPrincipal PrincipalSession principal) {
    return schoolService.getProfile(principal.schoolId());
  }

  @PutMapping("/api/v1/admin/principal-profile")
  PrincipalProfileResponse updateProfile(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody PrincipalProfileUpdateRequest request) {
    return schoolService.updateProfile(principal.schoolId(), principal.adminUserId(), request);
  }
}
