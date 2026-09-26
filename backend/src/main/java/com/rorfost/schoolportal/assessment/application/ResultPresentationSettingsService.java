package com.rorfost.schoolportal.assessment.application;

import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsRequest;
import com.rorfost.schoolportal.assessment.api.ResultPresentationSettingsResponse;
import com.rorfost.schoolportal.assessment.domain.ResultPresentationSettings;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultPresentationSettingsService {
  private final ResultPresentationSettingsRepository settings;

  public ResultPresentationSettingsService(ResultPresentationSettingsRepository settings) {
    this.settings = settings;
  }

  @Transactional(readOnly = true)
  public ResultPresentationSettingsResponse get(UUID schoolId) {
    return ResultPresentationSettingsResponse.from(
        settings.findById(schoolId).orElseGet(() -> new ResultPresentationSettings(schoolId)));
  }

  @Transactional
  public ResultPresentationSettingsResponse update(
      UUID schoolId, ResultPresentationSettingsRequest request) {
    if (request.gradeAMin().compareTo(request.gradeBMin()) < 0
        || request.gradeBMin().compareTo(request.gradeCMin()) < 0
        || request.gradeCMin().compareTo(request.gradeDMin()) < 0) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "result_grading_policy_invalid");
    }
    ResultPresentationSettings value =
        settings.findById(schoolId).orElseGet(() -> new ResultPresentationSettings(schoolId));
    value.update(
        request.resultDate(),
        trim(request.footerLineOne()),
        trim(request.footerLineTwo()),
        request.gradeAMin(),
        request.gradeBMin(),
        request.gradeCMin(),
        request.gradeDMin());
    return ResultPresentationSettingsResponse.from(settings.save(value));
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }
}
