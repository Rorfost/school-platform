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
<<<<<<< HEAD
    BigDecimal gradeDMin,
    String principalSignatureUrl,
    String classTeacherName,
    String classTeacherSignatureUrl) {
=======
    BigDecimal gradeDMin) {
>>>>>>> baaa4954004ce590e678a54c0862bfd396a37505
  public static ResultPresentationSettingsResponse from(ResultPresentationSettings value) {
    return new ResultPresentationSettingsResponse(
        value.getResultDate(),
        value.getFooterLineOne(),
        value.getFooterLineTwo(),
        value.getGradeAMin(),
        value.getGradeBMin(),
        value.getGradeCMin(),
<<<<<<< HEAD
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
=======
        value.getGradeDMin());
>>>>>>> baaa4954004ce590e678a54c0862bfd396a37505
  }
}
