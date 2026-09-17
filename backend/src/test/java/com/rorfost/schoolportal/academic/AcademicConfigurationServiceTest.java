package com.rorfost.schoolportal.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.api.StandardSubjectsUpdateRequest;
import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.util.List;
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
  void createsTheEightPrimaryStandardsOnlyWhenTheyAreMissing() {
    UUID schoolId = UUID.randomUUID();
    when(standards.existsBySchoolIdAndCode(any(), any())).thenReturn(false);

    service.ensureDefaultStandards(schoolId);

    ArgumentCaptor<Standard> saved = ArgumentCaptor.forClass(Standard.class);
    verify(standards, Mockito.times(8)).save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(Standard::getCode, Standard::getDisplayName)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("STD_1", "Standard 1"),
            org.assertj.core.groups.Tuple.tuple("STD_2", "Standard 2"),
            org.assertj.core.groups.Tuple.tuple("STD_3", "Standard 3"),
            org.assertj.core.groups.Tuple.tuple("STD_4", "Standard 4"),
            org.assertj.core.groups.Tuple.tuple("STD_5", "Standard 5"),
            org.assertj.core.groups.Tuple.tuple("STD_6", "Standard 6"),
            org.assertj.core.groups.Tuple.tuple("STD_7", "Standard 7"),
            org.assertj.core.groups.Tuple.tuple("STD_8", "Standard 8"));
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
}
