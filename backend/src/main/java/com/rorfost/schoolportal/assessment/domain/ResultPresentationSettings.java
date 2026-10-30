package com.rorfost.schoolportal.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "result_presentation_settings")
public class ResultPresentationSettings {
    @Id
    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "result_date")
    private LocalDate resultDate;

    @Column(name = "result_place")
    private String resultPlace;

    @Column(name = "footer_line_one")
    private String footerLineOne;

    @Column(name = "footer_line_two")
    private String footerLineTwo;

    @Column(name = "ekam_footer_line_one")
    private String ekamFooterLineOne;

    @Column(name = "ekam_footer_line_two")
    private String ekamFooterLineTwo;

    @Column(name = "ekam_result_date")
    private LocalDate ekamResultDate;

    @Column(name = "ekam_result_place")
    private String ekamResultPlace;

    @Column(name = "ekam_result_sheet_title")
    private String ekamResultSheetTitle;

    @Column(name = "grade_a_min")
    private BigDecimal gradeAMin = BigDecimal.valueOf(80);

    @Column(name = "grade_b_min")
    private BigDecimal gradeBMin = BigDecimal.valueOf(65);

    @Column(name = "grade_c_min")
    private BigDecimal gradeCMin = BigDecimal.valueOf(50);

    @Column(name = "grade_d_min")
    private BigDecimal gradeDMin = BigDecimal.valueOf(35);

    @Column(name = "result_sheet_title")
    private String resultSheetTitle;

    @Convert(converter = GradeBandsConverter.class)
    @Column(name = "grade_bands", columnDefinition = "jsonb")
    private List<GradeBand> gradeBands = defaultGradeBands();

    public static List<GradeBand> defaultGradeBands() {
        return List.of(
                new GradeBand("A", BigDecimal.valueOf(80)),
                new GradeBand("B", BigDecimal.valueOf(65)),
                new GradeBand("C", BigDecimal.valueOf(50)),
                new GradeBand("D", BigDecimal.valueOf(35)),
                new GradeBand("F", BigDecimal.ZERO));
    }

    protected ResultPresentationSettings() {}

    public ResultPresentationSettings(UUID schoolId) {
        this.schoolId = schoolId;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public LocalDate getResultDate() {
        return resultDate;
    }

    public String getResultPlace() {
        return resultPlace;
    }

    public String getFooterLineOne() {
        return footerLineOne;
    }

    public String getFooterLineTwo() {
        return footerLineTwo;
    }

    public String getEkamFooterLineOne() {
        return ekamFooterLineOne;
    }

    public String getEkamFooterLineTwo() {
        return ekamFooterLineTwo;
    }

    public LocalDate getEkamResultDate() {
        return ekamResultDate;
    }

    public String getEkamResultPlace() {
        return ekamResultPlace;
    }

    public String getEkamResultSheetTitle() {
        return ekamResultSheetTitle;
    }

    public BigDecimal getGradeAMin() {
        return gradeAMin;
    }

    public BigDecimal getGradeBMin() {
        return gradeBMin;
    }

    public BigDecimal getGradeCMin() {
        return gradeCMin;
    }

    public BigDecimal getGradeDMin() {
        return gradeDMin;
    }

    public String getResultSheetTitle() {
        return resultSheetTitle;
    }

    public List<GradeBand> getGradeBands() {
        return (gradeBands == null || gradeBands.isEmpty()) ? defaultGradeBands() : gradeBands;
    }

    public void update(
            LocalDate resultDate,
            String resultPlace,
            String footerLineOne,
            String footerLineTwo,
            String ekamFooterLineOne,
            String ekamFooterLineTwo,
            BigDecimal gradeAMin,
            BigDecimal gradeBMin,
            BigDecimal gradeCMin,
            BigDecimal gradeDMin,
            List<GradeBand> gradeBands,
            String resultSheetTitle,
            LocalDate ekamResultDate,
            String ekamResultPlace,
            String ekamResultSheetTitle) {
        this.resultDate = resultDate;
        this.resultPlace = resultPlace;
        this.footerLineOne = footerLineOne;
        this.footerLineTwo = footerLineTwo;
        this.ekamFooterLineOne = ekamFooterLineOne;
        this.ekamFooterLineTwo = ekamFooterLineTwo;
        this.gradeAMin = gradeAMin;
        this.gradeBMin = gradeBMin;
        this.gradeCMin = gradeCMin;
        this.gradeDMin = gradeDMin;
        if (gradeBands != null && !gradeBands.isEmpty()) {
            this.gradeBands = gradeBands;
        }
        this.resultSheetTitle = resultSheetTitle;
        this.ekamResultDate = ekamResultDate;
        this.ekamResultPlace = ekamResultPlace;
        this.ekamResultSheetTitle = ekamResultSheetTitle;
    }

    public void update(
            LocalDate resultDate,
            String resultPlace,
            String footerLineOne,
            String footerLineTwo,
            String ekamFooterLineOne,
            String ekamFooterLineTwo,
            BigDecimal gradeAMin,
            BigDecimal gradeBMin,
            BigDecimal gradeCMin,
            BigDecimal gradeDMin,
            String resultSheetTitle,
            LocalDate ekamResultDate,
            String ekamResultPlace,
            String ekamResultSheetTitle) {
        update(
                resultDate,
                resultPlace,
                footerLineOne,
                footerLineTwo,
                ekamFooterLineOne,
                ekamFooterLineTwo,
                gradeAMin,
                gradeBMin,
                gradeCMin,
                gradeDMin,
                null,
                resultSheetTitle,
                ekamResultDate,
                ekamResultPlace,
                ekamResultSheetTitle);
    }
}
