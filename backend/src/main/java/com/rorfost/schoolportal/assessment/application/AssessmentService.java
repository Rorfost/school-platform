package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.assessment.api.AssessmentRequest;
import com.rorfost.schoolportal.assessment.api.AssessmentResponse;
import com.rorfost.schoolportal.assessment.domain.Assessment;
import com.rorfost.schoolportal.assessment.domain.AssessmentStatus;
import com.rorfost.schoolportal.assessment.domain.AssessmentSubject;
import com.rorfost.schoolportal.assessment.repository.AssessmentRepository;
import com.rorfost.schoolportal.assessment.repository.AssessmentSubjectRepository;
import com.rorfost.schoolportal.assessment.repository.AssessmentTypeRepository;
import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {
  private final AssessmentRepository assessments;
  private final AssessmentSubjectRepository assessmentSubjects;
  private final AssessmentTypeRepository assessmentTypes;
  private final AcademicYearRepository academicYears;
  private final StandardRepository standards;
  private final StandardSubjectRepository standardSubjects;
  private final AuditLogService audit;

  public AssessmentService(
      AssessmentRepository assessments,
      AssessmentSubjectRepository assessmentSubjects,
      AssessmentTypeRepository assessmentTypes,
      AcademicYearRepository academicYears,
      StandardRepository standards,
      StandardSubjectRepository standardSubjects,
      AuditLogService audit) {
    this.assessments = assessments;
    this.assessmentSubjects = assessmentSubjects;
    this.assessmentTypes = assessmentTypes;
    this.academicYears = academicYears;
    this.standards = standards;
    this.standardSubjects = standardSubjects;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public List<AssessmentResponse> list(UUID schoolId) {
    return assessments.findBySchoolIdOrderByUpdatedAtDesc(schoolId).stream()
        .map(this::response)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AssessmentResponse> publicList(UUID schoolId, UUID yearId, UUID standardId) {
    return assessments
        .findBySchoolIdAndAcademicYearIdAndStandardIdAndStatusOrderByPublishedAtDesc(
            schoolId, yearId, standardId, AssessmentStatus.PUBLISHED)
        .stream()
        .map(this::response)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<com.rorfost.schoolportal.assessment.api.AssessmentTypeResponse> types() {
    return assessmentTypes.findByIsActiveTrueOrderBySortOrder().stream()
        .map(com.rorfost.schoolportal.assessment.api.AssessmentTypeResponse::from)
        .toList();
  }

  @Transactional
  public AssessmentResponse create(UUID schoolId, UUID actorId, AssessmentRequest request) {
    validateScope(schoolId, request);
    validateSubjects(schoolId, request);
    Assessment item =
        assessments.save(
            new Assessment(
                schoolId,
                request.academicYearId(),
                request.standardId(),
                request.assessmentTypeId(),
                request.title().trim()));
    item.update(request.title().trim(), trim(request.description()), request.assessmentDate());
    saveSubjects(item, request.subjects());
    audit(schoolId, actorId, AuditAction.ASSESSMENT_CREATED, item.getId());
    return response(item);
  }

  @Transactional
  public AssessmentResponse update(
      UUID schoolId, UUID actorId, UUID id, AssessmentRequest request) {
    Assessment item = require(schoolId, id);
    if (item.getStatus() == AssessmentStatus.ARCHIVED) throw conflict("assessment_archived");
    if (!item.getAcademicYearId().equals(request.academicYearId())
        || !item.getStandardId().equals(request.standardId())
        || !item.getAssessmentTypeId().equals(request.assessmentTypeId()))
      throw conflict("assessment_scope_immutable");
    validateSubjects(schoolId, request);
    item.update(request.title().trim(), trim(request.description()), request.assessmentDate());
    assessmentSubjects.deleteAll(
        assessmentSubjects.findByAssessmentIdOrderByCreatedAt(item.getId()));
    saveSubjects(item, request.subjects());
    audit(schoolId, actorId, AuditAction.ASSESSMENT_UPDATED, item.getId());
    return response(item);
  }

  @Transactional
  public AssessmentResponse publish(UUID schoolId, UUID actorId, UUID id) {
    Assessment item = require(schoolId, id);
    if (item.getStatus() == AssessmentStatus.ARCHIVED) throw conflict("assessment_archived");
    List<AssessmentSubject> subjects = assessmentSubjects.findByAssessmentIdOrderByCreatedAt(id);
    if (subjects.isEmpty()
        || subjects.stream()
            .anyMatch(value -> value.getMaximumMarks() == null || value.getPassingMarks() == null))
      throw new DomainException(HttpStatus.BAD_REQUEST, "assessment_subjects_incomplete");
    item.publish(Instant.now());
    audit(schoolId, actorId, AuditAction.ASSESSMENT_PUBLISHED, id);
    return response(item);
  }

  @Transactional
  public AssessmentResponse archive(UUID schoolId, UUID actorId, UUID id) {
    Assessment item = require(schoolId, id);
    item.archive(Instant.now());
    audit(schoolId, actorId, AuditAction.ASSESSMENT_ARCHIVED, id);
    return response(item);
  }

  private void validateScope(UUID schoolId, AssessmentRequest request) {
    if (academicYears
        .findById(request.academicYearId())
        .filter(
            value ->
                value.getSchoolId().equals(schoolId)
                    && value.getStatus() == AcademicYearStatus.CURRENT)
        .isEmpty()) throw new DomainException(HttpStatus.BAD_REQUEST, "academic_year_invalid");
    if (standards
        .findByIdAndSchoolId(request.standardId(), schoolId)
        .filter(value -> !value.isArchived())
        .isEmpty()) throw new DomainException(HttpStatus.BAD_REQUEST, "standard_invalid");
    if (assessmentTypes.findByIdAndIsActiveTrue(request.assessmentTypeId()).isEmpty())
      throw new DomainException(HttpStatus.BAD_REQUEST, "assessment_type_invalid");
  }

  private void validateSubjects(UUID schoolId, AssessmentRequest request) {
    if (request.subjects().stream()
            .map(AssessmentRequest.AssessmentSubjectRequest::standardSubjectId)
            .distinct()
            .count()
        != request.subjects().size()) throw conflict("assessment_subject_duplicate");
    for (AssessmentRequest.AssessmentSubjectRequest value : request.subjects()) {
      if (value.maximumMarks().compareTo(BigDecimal.ZERO) <= 0
          || value.passingMarks().compareTo(BigDecimal.ZERO) < 0
          || value.passingMarks().compareTo(value.maximumMarks()) > 0)
        throw new DomainException(HttpStatus.BAD_REQUEST, "assessment_marks_invalid");
      if (standardSubjects
          .findByIdAndSchoolId(value.standardSubjectId(), schoolId)
          .filter(mapping -> mapping.getStandardId().equals(request.standardId()))
          .isEmpty())
        throw new DomainException(HttpStatus.BAD_REQUEST, "assessment_subject_invalid");
    }
  }

  private void saveSubjects(
      Assessment item, List<AssessmentRequest.AssessmentSubjectRequest> values) {
    values.forEach(
        value ->
            assessmentSubjects.save(
                new AssessmentSubject(
                    item.getSchoolId(),
                    item.getId(),
                    item.getStandardId(),
                    value.standardSubjectId(),
                    value.maximumMarks(),
                    value.passingMarks())));
  }

  private Assessment require(UUID schoolId, UUID id) {
    return assessments
        .findByIdAndSchoolId(id, schoolId)
        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "assessment_not_found"));
  }

  private AssessmentResponse response(Assessment item) {
    return AssessmentResponse.from(
        item, assessmentSubjects.findByAssessmentIdOrderByCreatedAt(item.getId()));
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private DomainException conflict(String code) {
    return new DomainException(HttpStatus.CONFLICT, code);
  }

  private void audit(UUID schoolId, UUID actorId, AuditAction action, UUID id) {
    audit.record(schoolId, actorId, action, "ASSESSMENT", id, MDC.get("requestId"));
  }
}
