package com.rorfost.schoolportal.academic.api;

import java.util.List;

/** The focused admin view of the curriculum: each standard with its available subjects. */
public record AcademicSetupResponse(
    List<StandardSubjectsResponse> standards, List<SubjectResponse> subjects) {
  public record StandardSubjectsResponse(
      StandardResponse standard, List<SubjectResponse> subjects) {}
}
