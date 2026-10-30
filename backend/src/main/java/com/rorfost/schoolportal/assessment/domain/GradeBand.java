package com.rorfost.schoolportal.assessment.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record GradeBand(String label, BigDecimal minPercentage) {
    public GradeBand {
        Objects.requireNonNull(label, "label must not be null");
        Objects.requireNonNull(minPercentage, "minPercentage must not be null");
    }
}
