package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsRequest;
import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsResponse;
import com.rorfost.schoolportal.assessment.domain.GradeBand;
import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ResultPresentationSettingsService {
    private final ResultPresentationSettingsRepository settings;

    public ResultPresentationSettingsService(ResultPresentationSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public ResultPresentationSettingsResponse get(UUID schoolId) {
        return ResultPresentationSettingsResponse.from(
                settings.findById(schoolId)
                        .orElseGet(() -> new ResultPresentationSettings(schoolId)));
    }

    @Transactional
    public ResultPresentationSettingsResponse update(
            UUID schoolId, ResultPresentationSettingsRequest request) {
        if (request.gradeAMin().compareTo(request.gradeBMin()) < 0
                || request.gradeBMin().compareTo(request.gradeCMin()) < 0
                || request.gradeCMin().compareTo(request.gradeDMin()) < 0) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "result_grading_policy_invalid");
        }

        List<GradeBand> validatedBands = validateAndNormalizeGradeBands(request.gradeBands());

        ResultPresentationSettings value =
                settings.findById(schoolId)
                        .orElseGet(() -> new ResultPresentationSettings(schoolId));
        value.update(
                request.resultDate(),
                trim(request.resultPlace()),
                trim(request.footerLineOne()),
                trim(request.footerLineTwo()),
                trim(request.ekamFooterLineOne()),
                trim(request.ekamFooterLineTwo()),
                request.gradeAMin(),
                request.gradeBMin(),
                request.gradeCMin(),
                request.gradeDMin(),
                validatedBands,
                trim(request.resultSheetTitle()),
                request.ekamResultDate(),
                trim(request.ekamResultPlace()),
                trim(request.ekamResultSheetTitle()));
        return ResultPresentationSettingsResponse.from(settings.save(value));
    }

    private List<GradeBand> validateAndNormalizeGradeBands(List<GradeBand> bands) {
        if (bands == null || bands.isEmpty()) {
            return null;
        }

        Set<String> normalizedLabels = new HashSet<>();
        Set<BigDecimal> thresholds = new HashSet<>();
        boolean hasZeroCatchAll = false;

        for (GradeBand band : bands) {
            if (band.label() == null || band.label().isBlank()) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_label_blank");
            }
            String trimmedLabel = band.label().trim();
            if (trimmedLabel.length() > 20) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_label_too_long");
            }
            String norm = trimmedLabel.toUpperCase(Locale.ROOT);
            if (!normalizedLabels.add(norm)) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_duplicate_label");
            }

            if (band.minPercentage() == null) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_threshold_null");
            }
            BigDecimal pct = band.minPercentage();
            if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST, "grade_band_threshold_out_of_range");
            }
            if (!thresholds.add(pct)) {
                throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_duplicate_threshold");
            }

            if (pct.compareTo(BigDecimal.ZERO) == 0) {
                hasZeroCatchAll = true;
            }
        }

        if (!hasZeroCatchAll) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "grade_band_missing_zero_catch_all");
        }

        // Return sorted descending by minPercentage
        return bands.stream()
                .map(b -> new GradeBand(b.label().trim(), b.minPercentage()))
                .sorted((a, b) -> b.minPercentage().compareTo(a.minPercentage()))
                .toList();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
