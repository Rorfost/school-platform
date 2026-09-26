package com.rorfost.schoolportal.assessment.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ResultPresentationSettingsRequest(
    LocalDate resultDate,
    @Size(max = 1000) String footerLineOne,
    @Size(max = 1000) String footerLineTwo,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gradeAMin,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gradeBMin,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gradeCMin,
    @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal gradeDMin) {}
