package com.rorfost.schoolportal.academic.application;

import com.rorfost.schoolportal.academic.api.AcademicYearRequest;
import com.rorfost.schoolportal.academic.api.AcademicYearResponse;
import com.rorfost.schoolportal.academic.api.StandardRequest;
import com.rorfost.schoolportal.academic.api.StandardResponse;
import com.rorfost.schoolportal.academic.api.StandardSubjectRequest;
import com.rorfost.schoolportal.academic.api.StandardSubjectResponse;
import com.rorfost.schoolportal.academic.api.SubjectRequest;
import com.rorfost.schoolportal.academic.api.SubjectResponse;
import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicConfigurationService {
  private final AcademicYearRepository academicYears;
  private final StandardRepository standards;
  private final SubjectRepository subjects;
  private final StandardSubjectRepository standardSubjects;
  private final AuditLogService audit;

  public AcademicConfigurationService(
      AcademicYearRepository academicYears,
      StandardRepository standards,
      SubjectRepository subjects,
      StandardSubjectRepository standardSubjects,
      AuditLogService audit) {
    this.academicYears = academicYears;
    this.standards = standards;
    this.subjects = subjects;
    this.standardSubjects = standardSubjects;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public List<AcademicYearResponse> years(UUID schoolId) {
    return academicYears.findBySchoolIdOrderByStartsOnDesc(schoolId).stream()
        .map(AcademicYearResponse::from)
        .toList();
  }

  @Transactional
  public AcademicYearResponse createYear(UUID schoolId, UUID actorId, AcademicYearRequest request) {
    validateDates(request);
    String name = request.name().trim();
    if (academicYears.existsBySchoolIdAndName(schoolId, name))
      throw conflict("academic_year_duplicate");
    AcademicYear year =
        new AcademicYear(
            schoolId,
            name,
            request.startsOn(),
            request.endsOn(),
            request.current() ? AcademicYearStatus.CURRENT : AcademicYearStatus.ARCHIVED,
            request.current() ? null : Instant.now());
    if (request.current()) replaceCurrent(schoolId, null);
    academicYears.save(year);
    audit(schoolId, actorId, AuditAction.ACADEMIC_YEAR_CREATED, "ACADEMIC_YEAR", year.getId());
    return AcademicYearResponse.from(year);
  }

  @Transactional
  public AcademicYearResponse updateYear(
      UUID schoolId, UUID actorId, UUID id, AcademicYearRequest request) {
    validateDates(request);
    AcademicYear year = requireYear(schoolId, id);
    if (year.isArchived()) throw conflict("academic_year_archived");
    year.update(request.name().trim(), request.startsOn(), request.endsOn());
    if (request.current()) replaceCurrent(schoolId, year.getId());
    audit(schoolId, actorId, AuditAction.ACADEMIC_YEAR_UPDATED, "ACADEMIC_YEAR", id);
    return AcademicYearResponse.from(year);
  }

  @Transactional
  public AcademicYearResponse markCurrent(UUID schoolId, UUID actorId, UUID id) {
    AcademicYear year = requireYear(schoolId, id);
    replaceCurrent(schoolId, id);
    year.markCurrent();
    audit(schoolId, actorId, AuditAction.ACADEMIC_YEAR_UPDATED, "ACADEMIC_YEAR", id);
    return AcademicYearResponse.from(year);
  }

  @Transactional
  public AcademicYearResponse archiveYear(UUID schoolId, UUID actorId, UUID id) {
    AcademicYear year = requireYear(schoolId, id);
    if (year.getStatus() == AcademicYearStatus.CURRENT)
      throw conflict("current_academic_year_cannot_be_archived");
    if (!year.isArchived()) year.archive(Instant.now());
    audit(schoolId, actorId, AuditAction.ACADEMIC_YEAR_ARCHIVED, "ACADEMIC_YEAR", id);
    return AcademicYearResponse.from(year);
  }

  @Transactional(readOnly = true)
  public List<StandardResponse> standards(UUID schoolId) {
    return standards.findBySchoolIdOrderBySortOrder(schoolId).stream()
        .map(StandardResponse::from)
        .toList();
  }

  @Transactional
  public StandardResponse createStandard(UUID schoolId, UUID actorId, StandardRequest request) {
    Standard standard =
        standards.save(
            new Standard(
                schoolId,
                request.code().trim(),
                request.displayName().trim(),
                request.sortOrder()));
    standard.update(
        standard.getCode(), standard.getDisplayName(), standard.getSortOrder(), request.archived());
    audit(schoolId, actorId, AuditAction.STANDARD_UPDATED, "STANDARD", standard.getId());
    return StandardResponse.from(standard);
  }

  @Transactional
  public StandardResponse updateStandard(
      UUID schoolId, UUID actorId, UUID id, StandardRequest request) {
    Standard standard = requireStandard(schoolId, id);
    standard.update(
        request.code().trim(),
        request.displayName().trim(),
        request.sortOrder(),
        request.archived());
    audit(schoolId, actorId, AuditAction.STANDARD_UPDATED, "STANDARD", id);
    return StandardResponse.from(standard);
  }

  @Transactional(readOnly = true)
  public List<SubjectResponse> subjects(UUID schoolId) {
    return subjects.findBySchoolIdOrderBySortOrder(schoolId).stream()
        .map(SubjectResponse::from)
        .toList();
  }

  @Transactional
  public SubjectResponse createSubject(UUID schoolId, UUID actorId, SubjectRequest request) {
    Subject subject =
        subjects.save(
            new Subject(
                schoolId,
                request.code().trim().toUpperCase(Locale.ROOT),
                request.name().trim(),
                request.sortOrder()));
    subject.update(
        subject.getCode(), subject.getName(), subject.getSortOrder(), request.archived());
    audit(schoolId, actorId, AuditAction.SUBJECT_UPDATED, "SUBJECT", subject.getId());
    return SubjectResponse.from(subject);
  }

  @Transactional
  public SubjectResponse updateSubject(
      UUID schoolId, UUID actorId, UUID id, SubjectRequest request) {
    Subject subject = requireSubject(schoolId, id);
    subject.update(
        request.code().trim().toUpperCase(Locale.ROOT),
        request.name().trim(),
        request.sortOrder(),
        request.archived());
    audit(schoolId, actorId, AuditAction.SUBJECT_UPDATED, "SUBJECT", id);
    return SubjectResponse.from(subject);
  }

  @Transactional(readOnly = true)
  public List<StandardSubjectResponse> mappings(UUID schoolId, UUID standardId) {
    return standardSubjects
        .findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId)
        .stream()
        .map(StandardSubjectResponse::from)
        .toList();
  }

  @Transactional
  public StandardSubjectResponse createMapping(
      UUID schoolId, UUID actorId, StandardSubjectRequest request) {
    if (requireStandard(schoolId, request.standardId()).isArchived()
        || requireSubject(schoolId, request.subjectId()).isArchived())
      throw conflict("archived_academic_item");
    if (standardSubjects.existsByStandardIdAndSubjectId(request.standardId(), request.subjectId()))
      throw conflict("standard_subject_duplicate");
    StandardSubject mapping =
        standardSubjects.save(
            new StandardSubject(
                schoolId, request.standardId(), request.subjectId(), request.sortOrder()));
    audit(schoolId, actorId, AuditAction.SUBJECT_UPDATED, "STANDARD_SUBJECT", mapping.getId());
    return StandardSubjectResponse.from(mapping);
  }

  @Transactional
  public StandardSubjectResponse updateMapping(
      UUID schoolId, UUID actorId, UUID id, StandardSubjectRequest request) {
    StandardSubject mapping =
        standardSubjects
            .findByIdAndSchoolId(id, schoolId)
            .orElseThrow(() -> notFound("standard_subject_not_found"));
    if (!mapping.getStandardId().equals(request.standardId())
        || !mapping.getSubjectId().equals(request.subjectId()))
      throw conflict("standard_subject_immutable");
    mapping.setSortOrder(request.sortOrder());
    audit(schoolId, actorId, AuditAction.SUBJECT_UPDATED, "STANDARD_SUBJECT", id);
    return StandardSubjectResponse.from(mapping);
  }

  private void replaceCurrent(UUID schoolId, UUID keepId) {
    academicYears
        .findBySchoolIdAndStatus(schoolId, AcademicYearStatus.CURRENT)
        .filter(current -> !current.getId().equals(keepId))
        .ifPresent(current -> current.archive(Instant.now()));
  }

  private void validateDates(AcademicYearRequest request) {
    if (!request.endsOn().isAfter(request.startsOn()))
      throw new DomainException(HttpStatus.BAD_REQUEST, "academic_year_dates_invalid");
  }

  private AcademicYear requireYear(UUID schoolId, UUID id) {
    return academicYears
        .findById(id)
        .filter(value -> value.getSchoolId().equals(schoolId))
        .orElseThrow(() -> notFound("academic_year_not_found"));
  }

  private Standard requireStandard(UUID schoolId, UUID id) {
    return standards
        .findByIdAndSchoolId(id, schoolId)
        .orElseThrow(() -> notFound("standard_not_found"));
  }

  private Subject requireSubject(UUID schoolId, UUID id) {
    return subjects
        .findByIdAndSchoolId(id, schoolId)
        .orElseThrow(() -> notFound("subject_not_found"));
  }

  private DomainException notFound(String code) {
    return new DomainException(HttpStatus.NOT_FOUND, code);
  }

  private DomainException conflict(String code) {
    return new DomainException(HttpStatus.CONFLICT, code);
  }

  private void audit(
      UUID schoolId, UUID actorId, AuditAction action, String targetType, UUID targetId) {
    audit.record(schoolId, actorId, action, targetType, targetId, MDC.get("requestId"));
  }
}
