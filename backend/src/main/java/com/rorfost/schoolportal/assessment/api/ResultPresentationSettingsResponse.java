package com.rorfost.schoolportal.assessment.api;

import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultPresentationSettingsResponse(
    LocalDate resultDate,
    String resultSheetTitle,
    String footerLineOne,
    String footerLineTwo,
    String ekamFooterLineOne,
    String ekamFooterLineTwo,
    BigDecimal gradeAMin,
    BigDecimal gradeBMin,
    BigDecimal gradeCMin,
    BigDecimal gradeDMin,
    String principalSignatureUrl,
    String classTeacherName,
    String classTeacherSignatureUrl) {
  public static ResultPresentationSettingsResponse from(ResultPresentationSettings value) {
    return new ResultPresentationSettingsResponse(
        value.getResultDate(),
        value.getResultSheetTitle(),
        value.getFooterLineOne(),
        value.getFooterLineTwo(),
        value.getEkamFooterLineOne(),
        value.getEkamFooterLineTwo(),
        value.getGradeAMin(),
        value.getGradeBMin(),
        value.getGradeCMin(),
        value.getGradeDMin(),
        null,
        null,
        null);
  }

  public ResultPresentationSettingsResponse withSignatures(
      String principalSignatureUrl, String classTeacherName, String classTeacherSignatureUrl) {
    return new ResultPresentationSettingsResponse(
        resultDate,
        resultSheetTitle,
        footerLineOne,
        footerLineTwo,
        ekamFooterLineOne,
        ekamFooterLineTwo,
        gradeAMin,
        gradeBMin,
        gradeCMin,
        gradeDMin,
        principalSignatureUrl,
        classTeacherName,
        classTeacherSignatureUrl);
  }
}
