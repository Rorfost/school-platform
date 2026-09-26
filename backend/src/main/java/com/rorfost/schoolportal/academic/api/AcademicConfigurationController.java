package com.rorfost.schoolportal.academic.api;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
class AcademicConfigurationController {
  private final AcademicConfigurationService service;

  AcademicConfigurationController(AcademicConfigurationService service) {
    this.service = service;
  }

  @GetMapping("/academic-years")
  List<AcademicYearResponse> years(@AuthenticationPrincipal PrincipalSession principal) {
    return service.years(principal.schoolId());
  }

  @PostMapping("/academic-years")
  AcademicYearResponse createYear(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody AcademicYearRequest request) {
    return service.createYear(principal.schoolId(), principal.adminUserId(), request);
  }

  @PutMapping("/academic-years/{id}")
  AcademicYearResponse updateYear(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody AcademicYearRequest request) {
    return service.updateYear(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @PostMapping("/academic-years/{id}/current")
  AcademicYearResponse markCurrent(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.markCurrent(principal.schoolId(), principal.adminUserId(), id);
  }

  @PostMapping("/academic-years/{id}/archive")
  AcademicYearResponse archiveYear(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.archiveYear(principal.schoolId(), principal.adminUserId(), id);
  }

  @GetMapping("/standards")
  List<StandardResponse> standards(@AuthenticationPrincipal PrincipalSession principal) {
    return service.standards(principal.schoolId());
  }

  @GetMapping("/academic-setup")
  AcademicSetupResponse academicSetup(@AuthenticationPrincipal PrincipalSession principal) {
    return service.academicSetup(principal.schoolId());
  }

  @PostMapping("/standards")
  StandardResponse createStandard(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody StandardRequest request) {
    return service.createStandard(principal.schoolId(), principal.adminUserId(), request);
  }

  @PostMapping("/standards/catalog")
  StandardResponse createCatalogStandard(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody StandardNameRequest request) {
    return service.createCatalogStandard(principal.schoolId(), principal.adminUserId(), request);
  }

  @PutMapping("/standards/{id}")
  StandardResponse updateStandard(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody StandardRequest request) {
    return service.updateStandard(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @DeleteMapping("/standards/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteStandard(@AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    service.deleteStandard(principal.schoolId(), principal.adminUserId(), id);
  }

  @PostMapping("/standards/{id}/archive")
  StandardResponse archiveStandard(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.archiveStandard(principal.schoolId(), principal.adminUserId(), id);
  }

  @PutMapping("/standards/{id}/class-teacher")
  StandardResponse updateClassTeacher(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody ClassTeacherRequest request) {
    return service.updateClassTeacher(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @PostMapping(value = "/standards/{id}/class-teacher/signature", consumes = "multipart/form-data")
  StandardResponse replaceClassTeacherSignature(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @RequestParam MultipartFile file) {
    return service.replaceClassTeacherSignature(
        principal.schoolId(), principal.adminUserId(), id, file);
  }

  @GetMapping("/subjects")
  List<SubjectResponse> subjects(@AuthenticationPrincipal PrincipalSession principal) {
    return service.subjects(principal.schoolId());
  }

  @PostMapping("/subjects")
  SubjectResponse createSubject(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody SubjectRequest request) {
    return service.createSubject(principal.schoolId(), principal.adminUserId(), request);
  }

  @PostMapping("/subjects/catalog")
  SubjectResponse createCatalogSubject(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody SubjectNameRequest request) {
    return service.createCatalogSubject(principal.schoolId(), principal.adminUserId(), request);
  }

  @PutMapping("/subjects/{id}")
  SubjectResponse updateSubject(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody SubjectRequest request) {
    return service.updateSubject(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @DeleteMapping("/subjects/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void deleteSubject(@AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    service.deleteSubject(principal.schoolId(), principal.adminUserId(), id);
  }

  @PostMapping("/subjects/{id}/archive")
  SubjectResponse archiveSubject(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID id) {
    return service.archiveSubject(principal.schoolId(), principal.adminUserId(), id);
  }

  @GetMapping("/standards/{standardId}/subjects")
  List<StandardSubjectResponse> mappings(
      @AuthenticationPrincipal PrincipalSession principal, @PathVariable UUID standardId) {
    return service.mappings(principal.schoolId(), standardId);
  }

  @PostMapping("/standard-subjects")
  StandardSubjectResponse createMapping(
      @AuthenticationPrincipal PrincipalSession principal,
      @Valid @RequestBody StandardSubjectRequest request) {
    return service.createMapping(principal.schoolId(), principal.adminUserId(), request);
  }

  @PutMapping("/standard-subjects/{id}")
  StandardSubjectResponse updateMapping(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID id,
      @Valid @RequestBody StandardSubjectRequest request) {
    return service.updateMapping(principal.schoolId(), principal.adminUserId(), id, request);
  }

  @PutMapping("/standards/{standardId}/subjects")
  List<SubjectResponse> replaceStandardSubjects(
      @AuthenticationPrincipal PrincipalSession principal,
      @PathVariable UUID standardId,
      @Valid @RequestBody StandardSubjectsUpdateRequest request) {
    return service.replaceStandardSubjects(
        principal.schoolId(), principal.adminUserId(), standardId, request);
  }
}
