package com.rorfost.schoolportal.assessment.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamResultResponse(
    UUID id,
    UUID schoolId,
    String studentUid,
    String studentName,
    String standard,
    String studentClass,
    Integer rollNumber,
    String generalRegisterNumber,
    String birthDate,
    String totalWorkingDays,
    String attendedDays,
    Integer totalMarks,
    Integer obtainedMarks,
    BigDecimal percentage,
    String overallGrade,
    List<ExamResultSubjectResponse> subjects) {}
