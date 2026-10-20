package com.rorfost.schoolportal.featureflag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class FeatureFlagServiceTest {

    private final SchoolFeatureRepository repository = Mockito.mock(SchoolFeatureRepository.class);
    private FeatureFlagService service;

    private final UUID schoolA = UUID.randomUUID();
    private final UUID schoolB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FeatureFlagService(repository);
    }

    @Test
    void unconfiguredSchoolInheritsRegistryDefaults() {
        when(repository.findByIdSchoolId(schoolA)).thenReturn(Collections.emptyList());

        Map<String, Boolean> features = service.getFeaturesForSchool(schoolA);

        // Core School 1 features must be enabled by default
        assertThat(features.get(FeatureKey.GALLERY.name())).isTrue();
        assertThat(features.get(FeatureKey.MATERIALS.name())).isTrue();
        assertThat(features.get(FeatureKey.NOTICES.name())).isTrue();
        assertThat(features.get(FeatureKey.DOWNLOADS.name())).isTrue();
        assertThat(features.get(FeatureKey.EXAM_RESULTS.name())).isTrue();

        // Unimplemented features must be disabled by default
        assertThat(features.get(FeatureKey.TIMETABLE.name())).isFalse();
        assertThat(features.get(FeatureKey.ATTENDANCE.name())).isFalse();
    }

    @Test
    void explicitSchoolOverrideTakesPrecedence() {
        // School B disables GALLERY and enables TIMETABLE
        SchoolFeature disableGallery = new SchoolFeature(schoolB, FeatureKey.GALLERY, false);
        SchoolFeature enableTimetable = new SchoolFeature(schoolB, FeatureKey.TIMETABLE, true);

        when(repository.findByIdSchoolId(schoolB))
                .thenReturn(List.of(disableGallery, enableTimetable));
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolB, FeatureKey.GALLERY.name()))
                .thenReturn(Optional.of(disableGallery));
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolB, FeatureKey.TIMETABLE.name()))
                .thenReturn(Optional.of(enableTimetable));
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolB, FeatureKey.NOTICES.name()))
                .thenReturn(Optional.empty());

        Map<String, Boolean> features = service.getFeaturesForSchool(schoolB);

        assertThat(features.get(FeatureKey.GALLERY.name())).isFalse();
        assertThat(features.get(FeatureKey.TIMETABLE.name())).isTrue();
        assertThat(features.get(FeatureKey.NOTICES.name())).isTrue(); // fallback to default

        assertThat(service.isFeatureEnabled(schoolB, FeatureKey.GALLERY)).isFalse();
        assertThat(service.isFeatureEnabled(schoolB, FeatureKey.TIMETABLE)).isTrue();
        assertThat(service.isFeatureEnabled(schoolB, FeatureKey.NOTICES)).isTrue();
    }

    @Test
    void setSchoolFeatureSavesNewOverrideOrUpdatesExisting() {
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolA, FeatureKey.EXAM_RESULTS.name()))
                .thenReturn(Optional.empty());

        service.setSchoolFeature(schoolA, FeatureKey.EXAM_RESULTS, false);

        ArgumentCaptor<SchoolFeature> captor = ArgumentCaptor.forClass(SchoolFeature.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSchoolId()).isEqualTo(schoolA);
        assertThat(captor.getValue().getFeatureKey()).isEqualTo(FeatureKey.EXAM_RESULTS.name());
        assertThat(captor.getValue().isEnabled()).isFalse();
    }

    @Test
    void changingSchoolAFeatureDoesNotChangeSchoolBFlags() {
        // Disabling GALLERY for schoolA
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolA, FeatureKey.GALLERY.name()))
                .thenReturn(Optional.of(new SchoolFeature(schoolA, FeatureKey.GALLERY, false)));
        // schoolB has no override (defaults apply)
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolB, FeatureKey.GALLERY.name()))
                .thenReturn(Optional.empty());

        assertThat(service.isFeatureEnabled(schoolA, FeatureKey.GALLERY)).isFalse();
        assertThat(service.isFeatureEnabled(schoolB, FeatureKey.GALLERY)).isTrue();
    }

    @Test
    void setSchoolFeatureBySlugValidatesInputAndUpdatesFeatureWithAudit() {
        com.rorfost.schoolportal.school.repository.SchoolRepository schoolRepo =
                Mockito.mock(com.rorfost.schoolportal.school.repository.SchoolRepository.class);
        com.rorfost.schoolportal.audit.service.AuditLogService auditService =
                Mockito.mock(com.rorfost.schoolportal.audit.service.AuditLogService.class);

        FeatureFlagService fullService =
                new FeatureFlagService(repository, schoolRepo, auditService);

        com.rorfost.schoolportal.school.domain.School school =
                new com.rorfost.schoolportal.school.domain.School("School A", "school-a");
        org.springframework.test.util.ReflectionTestUtils.setField(school, "id", schoolA);

        when(schoolRepo.findBySlug("school-a")).thenReturn(Optional.of(school));
        when(schoolRepo.existsById(schoolA)).thenReturn(true);
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolA, "NOTICES"))
                .thenReturn(Optional.empty());

        fullService.setSchoolFeatureBySlug("school-a", "NOTICES", false);

        ArgumentCaptor<SchoolFeature> captor = ArgumentCaptor.forClass(SchoolFeature.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSchoolId()).isEqualTo(schoolA);
        assertThat(captor.getValue().getFeatureKey()).isEqualTo("NOTICES");
        assertThat(captor.getValue().isEnabled()).isFalse();

        verify(auditService)
                .record(
                        org.mockito.ArgumentMatchers.eq(schoolA),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.eq(
                                com.rorfost.schoolportal.audit.domain.AuditAction
                                        .SCHOOL_FEATURE_UPDATED),
                        org.mockito.ArgumentMatchers.eq("SCHOOL_FEATURE"),
                        org.mockito.ArgumentMatchers.eq(schoolA),
                        org.mockito.ArgumentMatchers.eq("OPERATOR_CLI"));
    }

    @Test
    void setSchoolFeatureBySlugRejectsInvalidSlugOrKey() {
        com.rorfost.schoolportal.school.repository.SchoolRepository schoolRepo =
                Mockito.mock(com.rorfost.schoolportal.school.repository.SchoolRepository.class);
        com.rorfost.schoolportal.audit.service.AuditLogService auditService =
                Mockito.mock(com.rorfost.schoolportal.audit.service.AuditLogService.class);

        FeatureFlagService fullService =
                new FeatureFlagService(repository, schoolRepo, auditService);

        when(schoolRepo.findBySlug("nonexistent")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> fullService.setSchoolFeatureBySlug("nonexistent", "NOTICES", true))
                .isInstanceOf(com.rorfost.schoolportal.common.exception.DomainException.class)
                .satisfies(
                        e ->
                                assertThat(
                                                ((com.rorfost.schoolportal.common.exception
                                                                        .DomainException)
                                                                e)
                                                        .getCode())
                                        .isEqualTo("school_not_found"));

        com.rorfost.schoolportal.school.domain.School school =
                new com.rorfost.schoolportal.school.domain.School("School A", "school-a");
        org.springframework.test.util.ReflectionTestUtils.setField(school, "id", schoolA);
        when(schoolRepo.findBySlug("school-a")).thenReturn(Optional.of(school));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                fullService.setSchoolFeatureBySlug(
                                        "school-a", "UNKNOWN_FEATURE", true))
                .isInstanceOf(com.rorfost.schoolportal.common.exception.DomainException.class)
                .satisfies(
                        e ->
                                assertThat(
                                                ((com.rorfost.schoolportal.common.exception
                                                                        .DomainException)
                                                                e)
                                                        .getCode())
                                        .isEqualTo("invalid_feature_key"));
    }
}
