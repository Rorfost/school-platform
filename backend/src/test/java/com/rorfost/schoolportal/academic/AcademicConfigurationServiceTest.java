package com.rorfost.schoolportal.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.api.StandardNameRequest;
import com.rorfost.schoolportal.academic.api.StandardSubjectsUpdateRequest;
import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class AcademicConfigurationServiceTest {
  private final AcademicYearRepository years = Mockito.mock(AcademicYearRepository.class);
  private final StandardRepository standards = Mockito.mock(StandardRepository.class);
  private final SubjectRepository subjects = Mockito.mock(SubjectRepository.class);
  private final StandardSubjectRepository mappings = Mockito.mock(StandardSubjectRepository.class);
  private final AcademicConfigurationService service =
      new AcademicConfigurationService(
          years, standards, subjects, mappings, Mockito.mock(AuditLogService.class));

  @Test
  void createsStandardWithServerManagedCodeAndNextDisplayOrder() {
    UUID schoolId = UUID.randomUUID();
    when(standards.existsBySchoolIdAndDisplayNameIgnoreCase(schoolId, "Standard 5"))
        .thenReturn(false);
    when(standards.findFirstBySchoolIdOrderBySortOrderDesc(schoolId))
        .thenReturn(Optional.of(new Standard(schoolId, "STD_4", "Standard 4", (short) 4)));
    when(standards.save(any(Standard.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.createCatalogStandard(
        schoolId, UUID.randomUUID(), new StandardNameRequest(" Standard 5 "));

    ArgumentCaptor<Standard> saved = ArgumentCaptor.forClass(Standard.class);
    verify(standards).save(saved.capture());
    assertThat(saved.getValue().getCode()).startsWith("STANDARD_");
    assertThat(saved.getValue().getDisplayName()).isEqualTo("Standard 5");
    assertThat(saved.getValue().getSortOrder()).isEqualTo((short) 5);
  }

  @Test
  void rejectsDuplicateStandardNamesBeforeSaving() {
    UUID schoolId = UUID.randomUUID();
    when(standards.existsBySchoolIdAndDisplayNameIgnoreCase(schoolId, "Standard 5"))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createCatalogStandard(
                    schoolId, UUID.randomUUID(), new StandardNameRequest("Standard 5")))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode()).isEqualTo("standard_duplicate"));
    verify(standards, Mockito.never()).save(any());
  }

  @Test
  void refusesToRemoveAMappingUsedByExistingContentOrMarks() {
    UUID schoolId = UUID.randomUUID();
    UUID standardId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Standard standard = new Standard(schoolId, "STD_3", "Standard 3", (short) 3);
    StandardSubject mapping = new StandardSubject(schoolId, standardId, subjectId, (short) 1);
    ReflectionTestUtils.setField(mapping, "id", UUID.randomUUID());

    when(standards.findByIdAndSchoolId(standardId, schoolId))
        .thenReturn(java.util.Optional.of(standard));
    when(mappings.findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId))
        .thenReturn(List.of(mapping));
    when(mappings.isReferenced(mapping.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.replaceStandardSubjects(
                    schoolId,
                    UUID.randomUUID(),
                    standardId,
                    new StandardSubjectsUpdateRequest(List.of())))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode())
                    .isEqualTo("standard_subject_in_use"));
  }

  @Test
  void deletesUnusedStandardAndItsUnusedMappings() {
    UUID schoolId = UUID.randomUUID();
    UUID standardId = UUID.randomUUID();
    Standard standard = new Standard(schoolId, "STD_5", "Standard 5", (short) 5);
    ReflectionTestUtils.setField(standard, "id", standardId);
    StandardSubject mapping =
        new StandardSubject(schoolId, standardId, UUID.randomUUID(), (short) 1);
    ReflectionTestUtils.setField(mapping, "id", UUID.randomUUID());
    when(standards.findByIdAndSchoolId(standardId, schoolId)).thenReturn(Optional.of(standard));
    when(mappings.findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId))
        .thenReturn(List.of(mapping));
    when(standards.isReferenced(standardId)).thenReturn(false);
    when(mappings.isReferenced(mapping.getId())).thenReturn(false);

    service.deleteStandard(schoolId, UUID.randomUUID(), standardId);

    verify(mappings).deleteAll(List.of(mapping));
    verify(standards).delete(standard);
  }

  @Test
  void refusesToDeleteReferencedStandard() {
    UUID schoolId = UUID.randomUUID();
    UUID standardId = UUID.randomUUID();
    Standard standard = new Standard(schoolId, "STD_5", "Standard 5", (short) 5);
    when(standards.findByIdAndSchoolId(standardId, schoolId)).thenReturn(Optional.of(standard));
    when(mappings.findBySchoolIdAndStandardIdOrderBySortOrder(schoolId, standardId))
        .thenReturn(List.of());
    when(standards.isReferenced(standardId)).thenReturn(true);

    assertThatThrownBy(() -> service.deleteStandard(schoolId, UUID.randomUUID(), standardId))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error -> assertThat(((DomainException) error).getCode()).isEqualTo("standard_in_use"));
    verify(standards, Mockito.never()).delete(any());
  }

  @Test
  void deletesUnusedSubjectAndRemovesOnlyItsMappings() {
    UUID schoolId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Subject subject = new Subject(schoolId, "MATHS", "Mathematics", (short) 1);
    ReflectionTestUtils.setField(subject, "id", subjectId);
    StandardSubject mapping =
        new StandardSubject(schoolId, UUID.randomUUID(), subjectId, (short) 1);
    ReflectionTestUtils.setField(mapping, "id", UUID.randomUUID());
    when(subjects.findByIdAndSchoolId(subjectId, schoolId)).thenReturn(Optional.of(subject));
    when(mappings.findBySchoolIdAndSubjectId(schoolId, subjectId)).thenReturn(List.of(mapping));
    when(mappings.isReferenced(mapping.getId())).thenReturn(false);

    service.deleteSubject(schoolId, UUID.randomUUID(), subjectId);

    verify(mappings).deleteAll(List.of(mapping));
    verify(subjects).delete(subject);
  }

  @Test
  void refusesToDeleteSubjectUsedByHistoricalContent() {
    UUID schoolId = UUID.randomUUID();
    UUID subjectId = UUID.randomUUID();
    Subject subject = new Subject(schoolId, "MATHS", "Mathematics", (short) 1);
    StandardSubject mapping =
        new StandardSubject(schoolId, UUID.randomUUID(), subjectId, (short) 1);
    ReflectionTestUtils.setField(mapping, "id", UUID.randomUUID());
    when(subjects.findByIdAndSchoolId(subjectId, schoolId)).thenReturn(Optional.of(subject));
    when(mappings.findBySchoolIdAndSubjectId(schoolId, subjectId)).thenReturn(List.of(mapping));
    when(mappings.isReferenced(mapping.getId())).thenReturn(true);

    assertThatThrownBy(() -> service.deleteSubject(schoolId, UUID.randomUUID(), subjectId))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error -> assertThat(((DomainException) error).getCode()).isEqualTo("subject_in_use"));
    verify(subjects, Mockito.never()).delete(any());
  }

  @Test
  void rejectsCrossSchoolSubjectDeletion() {
    UUID schoolId = UUID.randomUUID();

    assertThatThrownBy(() -> service.deleteSubject(schoolId, UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(DomainException.class)
        .satisfies(
            error ->
                assertThat(((DomainException) error).getCode()).isEqualTo("subject_not_found"));
  }
}
