package com.rorfost.schoolportal.assessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.assessment.api.AssessmentRequest;
import com.rorfost.schoolportal.assessment.domain.Assessment;
import com.rorfost.schoolportal.assessment.domain.AssessmentType;
import com.rorfost.schoolportal.assessment.repository.AssessmentRepository;
import com.rorfost.schoolportal.assessment.repository.AssessmentSubjectRepository;
import com.rorfost.schoolportal.assessment.repository.AssessmentTypeRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-tenant isolation tests for AssessmentService.
 *
 * <p>Validates that assessments, academic years, standards, and standard-subject mappings are
 * strictly bounded to the acting tenant's school. Foreign UUIDs from another school are rejected
 * before any write occurs.
 */
class AssessmentTenantIsolationTest {

    private AssessmentRepository assessments;
    private AssessmentSubjectRepository assessmentSubjects;
    private AssessmentTypeRepository assessmentTypes;
    private AcademicYearRepository academicYears;
    private StandardRepository standards;
    private StandardSubjectRepository standardSubjects;
    private AssessmentService service;

    private UUID schoolA;
    private UUID schoolB;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        assessments = mock(AssessmentRepository.class);
        assessmentSubjects = mock(AssessmentSubjectRepository.class);
        assessmentTypes = mock(AssessmentTypeRepository.class);
        academicYears = mock(AcademicYearRepository.class);
        standards = mock(StandardRepository.class);
        standardSubjects = mock(StandardSubjectRepository.class);

        service =
                new AssessmentService(
                        assessments,
                        assessmentSubjects,
                        assessmentTypes,
                        academicYears,
                        standards,
                        standardSubjects,
                        mock(AuditLogService.class));

        schoolA = UUID.randomUUID();
        schoolB = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Rejects create when academicYearId belongs to a different school")
    void create_foreignAcademicYear_rejected() {
        UUID foreignYearId = UUID.randomUUID(); // belongs to schoolB
        UUID standardId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();

        // Lookup with schoolA returns empty because year belongs to schoolB
        when(academicYears.findById(foreignYearId)).thenReturn(Optional.empty());

        AssessmentRequest request = buildRequest(foreignYearId, standardId, typeId);

        assertThatThrownBy(() -> service.create(schoolA, actorId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("academic_year_invalid"));

        verify(assessments, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rejects create when standardId belongs to a different school")
    void create_foreignStandard_rejected() {
        UUID yearId = UUID.randomUUID();
        UUID foreignStandardId = UUID.randomUUID(); // belongs to schoolB
        UUID typeId = UUID.randomUUID();

        AcademicYear year =
                new AcademicYear(
                        schoolA,
                        "2024-25",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2025, 3, 31),
                        AcademicYearStatus.CURRENT,
                        null);
        when(academicYears.findById(yearId)).thenReturn(Optional.of(year));
        // standard lookup for schoolA returns empty because it belongs to schoolB
        when(standards.findByIdAndSchoolId(foreignStandardId, schoolA))
                .thenReturn(Optional.empty());

        AssessmentRequest request = buildRequest(yearId, foreignStandardId, typeId);

        assertThatThrownBy(() -> service.create(schoolA, actorId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("standard_invalid"));

        verify(assessments, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rejects update when assessment ID is from a different school (cross-tenant)")
    void update_foreignAssessmentId_rejected() {
        UUID foreignAssessmentId = UUID.randomUUID(); // belongs to schoolB
        UUID yearId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();

        // schoolA does not own this assessment
        when(assessments.findByIdAndSchoolId(foreignAssessmentId, schoolA))
                .thenReturn(Optional.empty());

        AssessmentRequest request = buildRequest(yearId, standardId, typeId);

        assertThatThrownBy(() -> service.update(schoolA, actorId, foreignAssessmentId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("assessment_not_found"));

        verify(assessments, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Rejects publish when assessment ID is from a different school")
    void publish_foreignAssessmentId_rejected() {
        UUID foreignAssessmentId = UUID.randomUUID();

        when(assessments.findByIdAndSchoolId(foreignAssessmentId, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(schoolA, actorId, foreignAssessmentId))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("assessment_not_found"));
    }

    @Test
    @DisplayName("Rejects archive when assessment ID is from a different school")
    void archive_foreignAssessmentId_rejected() {
        UUID foreignAssessmentId = UUID.randomUUID();

        when(assessments.findByIdAndSchoolId(foreignAssessmentId, schoolA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(schoolA, actorId, foreignAssessmentId))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("assessment_not_found"));
    }

    @Test
    @DisplayName("Rejects create when standard-subject mapping belongs to a different school")
    void create_foreignStandardSubjectMapping_rejected() {
        UUID yearId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();
        UUID typeId = UUID.randomUUID();
        UUID foreignMappingId = UUID.randomUUID(); // owned by schoolB

        AcademicYear year =
                new AcademicYear(
                        schoolA,
                        "2024-25",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2025, 3, 31),
                        AcademicYearStatus.CURRENT,
                        null);
        Standard standard = new Standard(schoolA, "STD_5", "Standard 5", (short) 5);
        ReflectionTestUtils.setField(standard, "id", standardId);

        when(academicYears.findById(yearId)).thenReturn(Optional.of(year));
        when(standards.findByIdAndSchoolId(standardId, schoolA)).thenReturn(Optional.of(standard));
        AssessmentType mockAssessmentType = mock(AssessmentType.class);
        when(assessmentTypes.findByIdAndIsActiveTrue(typeId))
                .thenReturn(Optional.of(mockAssessmentType));
        // The mapping does not belong to schoolA
        when(standardSubjects.findByIdAndSchoolId(foreignMappingId, schoolA))
                .thenReturn(Optional.empty());

        AssessmentRequest.AssessmentSubjectRequest subjectRequest =
                new AssessmentRequest.AssessmentSubjectRequest(
                        foreignMappingId, new BigDecimal("100"), new BigDecimal("35"));

        AssessmentRequest request =
                new AssessmentRequest(
                        yearId,
                        standardId,
                        typeId,
                        "Unit Test",
                        null,
                        LocalDate.now(),
                        List.of(subjectRequest));

        assertThatThrownBy(() -> service.create(schoolA, actorId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("assessment_subject_invalid"));

        verify(assessments, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("list() returns only assessments belonging to the acting school")
    void list_returnsOnlySchoolOwnedAssessments() {
        UUID schoolAAssessmentId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();
        Assessment schoolAAssessment =
                new Assessment(schoolA, UUID.randomUUID(), standardId, UUID.randomUUID(), "Exam A");
        ReflectionTestUtils.setField(schoolAAssessment, "id", schoolAAssessmentId);

        when(assessments.findBySchoolIdOrderByUpdatedAtDesc(schoolA))
                .thenReturn(List.of(schoolAAssessment));
        when(assessmentSubjects.findByAssessmentIdAndSchoolIdOrderByCreatedAt(
                        schoolAAssessmentId, schoolA))
                .thenReturn(List.of());

        List<com.rorfost.schoolportal.assessment.api.AssessmentResponse> results =
                service.list(schoolA);

        assertThat(results).hasSize(1);
        // Verify repository was called with schoolA only — School B's assessments are never
        // fetched.
        org.mockito.Mockito.verify(assessments, never())
                .findBySchoolIdOrderByUpdatedAtDesc(schoolB);
        assertThat(results.get(0).standardId()).isEqualTo(standardId);
    }

    private static AssessmentRequest buildRequest(UUID yearId, UUID standardId, UUID typeId) {
        return new AssessmentRequest(
                yearId, standardId, typeId, "Test Assessment", null, LocalDate.now(), List.of());
    }
}
