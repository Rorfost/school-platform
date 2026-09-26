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
    BigDecimal gradeDMin,
    String principalSignatureUrl,
    String classTeacherName,
    String classTeacherSignatureUrl) {
  public static ResultPresentationSettingsResponse from(ResultPresentationSettings value) {
    return new ResultPresentationSettingsResponse(
        value.getResultDate(),
        value.getFooterLineOne(),
        value.getFooterLineTwo(),
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
        footerLineOne,
        footerLineTwo,
        gradeAMin,
        gradeBMin,
        gradeCMin,
        gradeDMin,
        principalSignatureUrl,
        classTeacherName,
        classTeacherSignatureUrl);
  }
}
