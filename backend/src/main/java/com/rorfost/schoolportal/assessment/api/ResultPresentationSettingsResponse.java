package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultPresentationSettingsResponse(
    LocalDate resultDate,
    String footerLineOne,
    String footerLineTwo,
    BigDecimal gradeAMin,
    BigDecimal gradeBMin,
    BigDecimal gradeCMin,
    BigDecimal gradeDMin) {
  public static ResultPresentationSettingsResponse from(ResultPresentationSettings value) {
    return new ResultPresentationSettingsResponse(
        value.getResultDate(),
        value.getFooterLineOne(),
        value.getFooterLineTwo(),
        value.getGradeAMin(),
        value.getGradeBMin(),
        value.getGradeCMin(),
        value.getGradeDMin());
  }
}
