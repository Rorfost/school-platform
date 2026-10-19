package com.rorfost.schoolportal.assessment.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsRequest;
import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsResponse;
import com.rorfost.schoolportal.assessment.domain.GradeBand;
import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ResultPresentationSettingsServiceTest {

  private ResultPresentationSettingsRepository repository;
  private ResultPresentationSettingsService service;
  private UUID schoolId;

  @BeforeEach
  void setUp() {
    repository = mock(ResultPresentationSettingsRepository.class);
    service = new ResultPresentationSettingsService(repository);
    schoolId = UUID.randomUUID();
  }

  @Test
  void get_shouldReturnDefaultsWhenSettingsNotFound() {
    when(repository.findById(schoolId)).thenReturn(Optional.empty());

    ResultPresentationSettingsResponse response = service.get(schoolId);

    assertNotNull(response);
    assertEquals(BigDecimal.valueOf(80), response.gradeAMin());
    assertEquals(BigDecimal.valueOf(65), response.gradeBMin());
    assertEquals(BigDecimal.valueOf(50), response.gradeCMin());
    assertEquals(BigDecimal.valueOf(35), response.gradeDMin());

    List<GradeBand> bands = response.gradeBands();
    assertNotNull(bands);
    assertEquals(5, bands.size());
    assertEquals("A", bands.get(0).label());
    assertEquals(BigDecimal.valueOf(80), bands.get(0).minPercentage());
    assertEquals("F", bands.get(4).label());
    assertEquals(BigDecimal.ZERO, bands.get(4).minPercentage());
  }

  @Test
  void update_withCustomGradeBands_shouldValidateNormalizeAndSave() {
    ResultPresentationSettings existing = new ResultPresentationSettings(schoolId);
    when(repository.findById(schoolId)).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    List<GradeBand> customBands =
        List.of(
            new GradeBand("Distinction", BigDecimal.valueOf(85)),
            new GradeBand("Pass", BigDecimal.valueOf(40)),
            new GradeBand("Fail", BigDecimal.ZERO),
            new GradeBand("FirstClass", BigDecimal.valueOf(65)));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            LocalDate.of(2026, 4, 15),
            "Dhadhana",
            "Annual Exam Sheet",
            "Note 1",
            "Note 2",
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            customBands,
            null,
            null,
            null);

    ResultPresentationSettingsResponse response = service.update(schoolId, request);

    ArgumentCaptor<ResultPresentationSettings> captor =
        ArgumentCaptor.forClass(ResultPresentationSettings.class);
    verify(repository).save(captor.capture());
    ResultPresentationSettings saved = captor.getValue();

    List<GradeBand> savedBands = saved.getGradeBands();
    assertEquals(4, savedBands.size());
    // Must be sorted descending by minPercentage
    assertEquals("Distinction", savedBands.get(0).label());
    assertEquals(BigDecimal.valueOf(85), savedBands.get(0).minPercentage());
    assertEquals("FirstClass", savedBands.get(1).label());
    assertEquals(BigDecimal.valueOf(65), savedBands.get(1).minPercentage());
    assertEquals("Pass", savedBands.get(2).label());
    assertEquals(BigDecimal.valueOf(40), savedBands.get(2).minPercentage());
    assertEquals("Fail", savedBands.get(3).label());
    assertEquals(BigDecimal.ZERO, savedBands.get(3).minPercentage());
  }

  @Test
  void update_shouldRejectGradeBandsWithoutZeroCatchAll() {
    when(repository.findById(schoolId))
        .thenReturn(Optional.of(new ResultPresentationSettings(schoolId)));

    List<GradeBand> noZeroBands =
        List.of(
            new GradeBand("A", BigDecimal.valueOf(80)), new GradeBand("B", BigDecimal.valueOf(50)));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            noZeroBands,
            null,
            null,
            null);

    DomainException ex =
        assertThrows(DomainException.class, () -> service.update(schoolId, request));
    assertEquals("grade_band_missing_zero_catch_all", ex.getCode());
  }

  @Test
  void update_shouldRejectDuplicateGradeLabels() {
    when(repository.findById(schoolId))
        .thenReturn(Optional.of(new ResultPresentationSettings(schoolId)));

    List<GradeBand> duplicateLabels =
        List.of(
            new GradeBand("A", BigDecimal.valueOf(80)),
            new GradeBand("a", BigDecimal.valueOf(60)),
            new GradeBand("F", BigDecimal.ZERO));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            duplicateLabels,
            null,
            null,
            null);

    DomainException ex =
        assertThrows(DomainException.class, () -> service.update(schoolId, request));
    assertEquals("grade_band_duplicate_label", ex.getCode());
  }

  @Test
  void update_shouldRejectDuplicateThresholds() {
    when(repository.findById(schoolId))
        .thenReturn(Optional.of(new ResultPresentationSettings(schoolId)));

    List<GradeBand> duplicateThresholds =
        List.of(
            new GradeBand("A", BigDecimal.valueOf(80)),
            new GradeBand("B", BigDecimal.valueOf(80)),
            new GradeBand("F", BigDecimal.ZERO));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            duplicateThresholds,
            null,
            null,
            null);

    DomainException ex =
        assertThrows(DomainException.class, () -> service.update(schoolId, request));
    assertEquals("grade_band_duplicate_threshold", ex.getCode());
  }

  @ParameterizedTest
  @ValueSource(doubles = {-5.0, 105.0})
  void update_shouldRejectOutOfRangeThresholds(double threshold) {
    when(repository.findById(schoolId))
        .thenReturn(Optional.of(new ResultPresentationSettings(schoolId)));

    List<GradeBand> outOfRange =
        List.of(
            new GradeBand("Invalid", BigDecimal.valueOf(threshold)),
            new GradeBand("F", BigDecimal.ZERO));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            outOfRange,
            null,
            null,
            null);

    DomainException ex =
        assertThrows(DomainException.class, () -> service.update(schoolId, request));
    assertEquals("grade_band_threshold_out_of_range", ex.getCode());
  }

  @Test
  void update_shouldRejectBlankLabel() {
    when(repository.findById(schoolId))
        .thenReturn(Optional.of(new ResultPresentationSettings(schoolId)));

    List<GradeBand> blankLabel =
        List.of(new GradeBand("   ", BigDecimal.valueOf(80)), new GradeBand("F", BigDecimal.ZERO));

    ResultPresentationSettingsRequest request =
        new ResultPresentationSettingsRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            BigDecimal.valueOf(80),
            BigDecimal.valueOf(65),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(35),
            blankLabel,
            null,
            null,
            null);

    DomainException ex =
        assertThrows(DomainException.class, () -> service.update(schoolId, request));
    assertEquals("grade_band_label_blank", ex.getCode());
  }
}
