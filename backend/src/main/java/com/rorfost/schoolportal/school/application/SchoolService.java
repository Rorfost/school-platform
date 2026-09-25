package com.rorfost.schoolportal.school.application;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.common.storage.StoredObject;
import com.rorfost.schoolportal.school.api.PrincipalProfileResponse;
import com.rorfost.schoolportal.school.api.PrincipalProfileUpdateRequest;
import com.rorfost.schoolportal.school.api.SchoolResponse;
import com.rorfost.schoolportal.school.api.SchoolUpdateRequest;
import com.rorfost.schoolportal.school.domain.PrincipalProfile;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.PrincipalProfileRepository;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolService {
  private static final Logger log = LoggerFactory.getLogger(SchoolService.class);
  private final SchoolRepository schoolRepository;
  private final PrincipalProfileRepository principalProfileRepository;
  private final AuditLogService auditLogService;
  private final StorageService storage;

  public SchoolService(
      SchoolRepository schoolRepository,
      PrincipalProfileRepository principalProfileRepository,
      AuditLogService auditLogService,
      StorageService storage) {
    this.schoolRepository = schoolRepository;
    this.principalProfileRepository = principalProfileRepository;
    this.auditLogService = auditLogService;
    this.storage = storage;
  }

  @Transactional(readOnly = true)
  public SchoolResponse get(UUID schoolId) {
    return schoolResponse(requireSchool(schoolId));
  }

  @Transactional(readOnly = true)
  public SchoolResponse getPublic() {
    return schoolResponse(requirePublicSchool());
  }

  @Transactional
  public SchoolResponse update(UUID schoolId, UUID actorId, SchoolUpdateRequest request) {
    School school = requireSchool(schoolId);
    school.update(
        request.name().trim(),
        trim(request.shortName()),
        trim(request.schoolCode()),
        trim(request.address()),
        trim(request.city()),
        trim(request.state()),
        trim(request.postalCode()),
        trim(request.email()),
        trim(request.phone()),
        trim(request.website()),
        trim(request.mapsUrl()),
        trim(request.about()),
        trim(request.establishedYear()),
        trim(request.medium()),
        trim(request.schoolType()));
    auditLogService.record(
        schoolId, actorId, AuditAction.SCHOOL_UPDATED, "SCHOOL", schoolId, MDC.get("requestId"));
    return schoolResponse(school);
  }

  public SchoolResponse replaceLogo(
      UUID schoolId, UUID actorId, org.springframework.web.multipart.MultipartFile file) {
    School school = requireSchool(schoolId);
    StoredObject uploaded = storage.uploadPublicImage("branding/" + school.getSlug(), file);
    String previousKey = school.getLogoObjectKey();
    try {
      school.changeLogo(uploaded.objectKey());
      schoolRepository.saveAndFlush(school);
    } catch (RuntimeException exception) {
      cleanUpNewLogo(uploaded);
      throw exception;
    }
    auditLogService.record(
        schoolId,
        actorId,
        AuditAction.SCHOOL_UPDATED,
        "SCHOOL_LOGO",
        schoolId,
        MDC.get("requestId"));
    deletePreviousLogo(previousKey);
    return schoolResponse(school);
  }

  public SchoolResponse removeLogo(UUID schoolId, UUID actorId) {
    School school = requireSchool(schoolId);
    String previousKey = school.getLogoObjectKey();
    if (previousKey == null) return schoolResponse(school);
    school.changeLogo(null);
    schoolRepository.saveAndFlush(school);
    auditLogService.record(
        schoolId,
        actorId,
        AuditAction.SCHOOL_UPDATED,
        "SCHOOL_LOGO",
        schoolId,
        MDC.get("requestId"));
    deletePreviousLogo(previousKey);
    return schoolResponse(school);
  }

  @Transactional(readOnly = true)
  public PrincipalProfileResponse getProfile(UUID schoolId) {
    return PrincipalProfileResponse.admin(
        principalProfileRepository
            .findBySchoolId(schoolId)
            .orElseThrow(
                () -> new DomainException(HttpStatus.NOT_FOUND, "principal_profile_not_found")));
  }

  @Transactional(readOnly = true)
  public PrincipalProfileResponse getPublicProfile() {
    PrincipalProfile profile =
        principalProfileRepository
            .findBySchoolId(requirePublicSchool().getId())
            .filter(PrincipalProfile::isPublic)
            .orElseThrow(
                () -> new DomainException(HttpStatus.NOT_FOUND, "principal_profile_not_found"));
    return PrincipalProfileResponse.publicView(profile);
  }

  @Transactional
  public PrincipalProfileResponse updateProfile(
      UUID schoolId, UUID actorId, PrincipalProfileUpdateRequest request) {
    PrincipalProfile profile =
        principalProfileRepository
            .findBySchoolId(schoolId)
            .orElseGet(
                () ->
                    principalProfileRepository.save(
                        new PrincipalProfile(schoolId, request.fullName().trim())));
    profile.update(
        request.fullName().trim(),
        trim(request.biography()),
        trim(request.qualification()),
        trim(request.designation()),
        trim(request.message()),
        trim(request.portraitObjectKey()),
        trim(request.email()),
        trim(request.phone()),
        request.isPublic(),
        request.isContactPublic());
    auditLogService.record(
        schoolId,
        actorId,
        AuditAction.SCHOOL_UPDATED,
        "PRINCIPAL_PROFILE",
        profile.getId(),
        MDC.get("requestId"));
    return PrincipalProfileResponse.admin(profile);
  }

  private School requireSchool(UUID schoolId) {
    return schoolRepository
        .findById(schoolId)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_found"));
  }

  private School requirePublicSchool() {
    return schoolRepository
        .findFirstByIsActiveTrueOrderByCreatedAtAsc()
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "school_not_configured"));
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private SchoolResponse schoolResponse(School school) {
    String logoUrl =
        school.getLogoObjectKey() == null ? null : storage.publicUrl(school.getLogoObjectKey());
    return SchoolResponse.from(school, logoUrl);
  }

  private void cleanUpNewLogo(StoredObject uploaded) {
    try {
      storage.delete(uploaded);
    } catch (RuntimeException cleanupFailure) {
      log.error("Could not clean up the newly uploaded school logo", cleanupFailure);
    }
  }

  private void deletePreviousLogo(String previousKey) {
    if (previousKey == null) return;
    try {
      storage.deletePublicObject(previousKey);
    } catch (RuntimeException cleanupFailure) {
      log.error("Could not delete the replaced school logo", cleanupFailure);
    }
  }
}
