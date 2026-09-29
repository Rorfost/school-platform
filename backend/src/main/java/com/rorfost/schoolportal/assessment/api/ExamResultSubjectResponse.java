package com.rorfost.schoolportal.assessment.api;

import java.util.UUID;

public record ExamResultSubjectResponse(
    UUID id,
    String subjectName,
    Integer maximumMarks,
    Integer obtainedMarks,
    String grade,
    String status,
    Integer sortOrder) {}
