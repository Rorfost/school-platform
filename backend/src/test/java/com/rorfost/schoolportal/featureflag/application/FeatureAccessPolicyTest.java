package com.rorfost.schoolportal.featureflag.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link FeatureAccessPolicy}.
 *
 * <p>Covers positive (feature enabled) and negative (feature disabled) scenarios for public and
 * admin enforcement on each of the five platform features: GALLERY, MATERIALS, NOTICES, DOWNLOADS,
 * EXAM_RESULTS.
 */
class FeatureAccessPolicyTest {

    private SchoolFeatureRepository repository;
    private FeatureAccessPolicy policy;

    private final UUID schoolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SchoolFeatureRepository.class);
        FeatureFlagService service = new FeatureFlagService(repository);
        policy = new FeatureAccessPolicy(service);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void enabledByDefault() {
        // No explicit override → registry defaults apply (all 5 core features are true)
        when(repository.findByIdSchoolIdAndIdFeatureKey(Mockito.eq(schoolId), Mockito.anyString()))
                .thenReturn(Optional.empty());
    }

    private void disableFeature(FeatureKey key) {
        SchoolFeature override = new SchoolFeature(schoolId, key, false);
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolId, key.name()))
                .thenReturn(Optional.of(override));
        when(repository.findByIdSchoolId(schoolId)).thenReturn(List.of(override));
    }

    // ── Public enforcement: 404 when disabled ─────────────────────────────

    @Test
    void requirePublicEnabled_gallery_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requirePublicEnabled(schoolId, FeatureKey.GALLERY))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublicEnabled_gallery_disabled_shouldThrow404() {
        disableFeature(FeatureKey.GALLERY);
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.GALLERY))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void requirePublicEnabled_materials_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requirePublicEnabled(schoolId, FeatureKey.MATERIALS))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublicEnabled_materials_disabled_shouldThrow404() {
        disableFeature(FeatureKey.MATERIALS);
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.MATERIALS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void requirePublicEnabled_notices_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requirePublicEnabled(schoolId, FeatureKey.NOTICES))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublicEnabled_notices_disabled_shouldThrow404() {
        disableFeature(FeatureKey.NOTICES);
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.NOTICES))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void requirePublicEnabled_downloads_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requirePublicEnabled(schoolId, FeatureKey.DOWNLOADS))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublicEnabled_downloads_disabled_shouldThrow404() {
        disableFeature(FeatureKey.DOWNLOADS);
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.DOWNLOADS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void requirePublicEnabled_examResults_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requirePublicEnabled(schoolId, FeatureKey.EXAM_RESULTS))
                .doesNotThrowAnyException();
    }

    @Test
    void requirePublicEnabled_examResults_disabled_shouldThrow404() {
        disableFeature(FeatureKey.EXAM_RESULTS);
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.EXAM_RESULTS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    // ── Admin enforcement: 403 when disabled ─────────────────────────────

    @Test
    void requireAdminEnabled_gallery_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requireAdminEnabled(schoolId, FeatureKey.GALLERY))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminEnabled_gallery_disabled_shouldThrow403() {
        disableFeature(FeatureKey.GALLERY);
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.GALLERY))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }

    @Test
    void requireAdminEnabled_materials_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requireAdminEnabled(schoolId, FeatureKey.MATERIALS))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminEnabled_materials_disabled_shouldThrow403() {
        disableFeature(FeatureKey.MATERIALS);
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.MATERIALS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }

    @Test
    void requireAdminEnabled_notices_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requireAdminEnabled(schoolId, FeatureKey.NOTICES))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminEnabled_notices_disabled_shouldThrow403() {
        disableFeature(FeatureKey.NOTICES);
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.NOTICES))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }

    @Test
    void requireAdminEnabled_downloads_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requireAdminEnabled(schoolId, FeatureKey.DOWNLOADS))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminEnabled_downloads_disabled_shouldThrow403() {
        disableFeature(FeatureKey.DOWNLOADS);
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.DOWNLOADS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }

    @Test
    void requireAdminEnabled_examResults_enabled_shouldNotThrow() {
        enabledByDefault();
        assertThatCode(() -> policy.requireAdminEnabled(schoolId, FeatureKey.EXAM_RESULTS))
                .doesNotThrowAnyException();
    }

    @Test
    void requireAdminEnabled_examResults_disabled_shouldThrow403() {
        disableFeature(FeatureKey.EXAM_RESULTS);
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.EXAM_RESULTS))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }

    // ── Cross-school isolation: disabling for school A must not affect school B ──

    @Test
    void disabledFeatureForSchoolA_doesNotAffectSchoolB() {
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();

        SchoolFeature disabledForA = new SchoolFeature(schoolA, FeatureKey.GALLERY, false);
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolA, FeatureKey.GALLERY.name()))
                .thenReturn(Optional.of(disabledForA));
        // School B has no overrides; falls back to default (enabled)
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolB, FeatureKey.GALLERY.name()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolA, FeatureKey.GALLERY))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");

        assertThatCode(() -> policy.requirePublicEnabled(schoolB, FeatureKey.GALLERY))
                .doesNotThrowAnyException();
    }

    // ── Unimplemented features default to disabled ────────────────────────

    @Test
    void requirePublicEnabled_timetable_defaultDisabled_shouldThrow404() {
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolId, FeatureKey.TIMETABLE.name()))
                .thenReturn(Optional.empty()); // no override → falls back to defaultEnabled=false
        assertThatThrownBy(() -> policy.requirePublicEnabled(schoolId, FeatureKey.TIMETABLE))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasFieldOrPropertyWithValue("code", "feature_not_found");
    }

    @Test
    void requireAdminEnabled_attendance_defaultDisabled_shouldThrow403() {
        when(repository.findByIdSchoolIdAndIdFeatureKey(schoolId, FeatureKey.ATTENDANCE.name()))
                .thenReturn(Optional.empty()); // no override → falls back to defaultEnabled=false
        assertThatThrownBy(() -> policy.requireAdminEnabled(schoolId, FeatureKey.ATTENDANCE))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN)
                .hasFieldOrPropertyWithValue("code", "feature_disabled");
    }
}
