package com.rorfost.schoolportal.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "result_presentation_settings")
public class ResultPresentationSettings {
  @Id
  @Column(name = "school_id")
  private UUID schoolId;

  @Column(name = "result_date")
  private LocalDate resultDate;

  @Column(name = "footer_line_one")
  private String footerLineOne;

  @Column(name = "footer_line_two")
  private String footerLineTwo;

  @Column(name = "grade_a_min")
  private BigDecimal gradeAMin = BigDecimal.valueOf(80);

  @Column(name = "grade_b_min")
  private BigDecimal gradeBMin = BigDecimal.valueOf(65);

  @Column(name = "grade_c_min")
  private BigDecimal gradeCMin = BigDecimal.valueOf(50);

  @Column(name = "grade_d_min")
  private BigDecimal gradeDMin = BigDecimal.valueOf(35);

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

  public String getFooterLineOne() {
    return footerLineOne;
  }

  public String getFooterLineTwo() {
    return footerLineTwo;
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

  public void update(
      LocalDate resultDate,
      String footerLineOne,
      String footerLineTwo,
      BigDecimal gradeAMin,
      BigDecimal gradeBMin,
      BigDecimal gradeCMin,
      BigDecimal gradeDMin) {
    this.resultDate = resultDate;
    this.footerLineOne = footerLineOne;
    this.footerLineTwo = footerLineTwo;
    this.gradeAMin = gradeAMin;
    this.gradeBMin = gradeBMin;
    this.gradeCMin = gradeCMin;
    this.gradeDMin = gradeDMin;
  }
}
