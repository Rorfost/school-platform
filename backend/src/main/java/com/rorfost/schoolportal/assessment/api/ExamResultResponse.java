package com.rorfost.schoolportal.assessment.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExamResultResponse(
    UUID id,
    UUID schoolId,
    UUID academicYearId,
    String studentName,
    String standard,
    Integer rollNumber,
    String generalRegisterNumber,
    String birthDate,
    Integer totalWorkingDays,
    Integer attendedDays,
    Integer totalMarks,
    Integer obtainedMarks,
    BigDecimal percentage,
    String overallGrade,
    List<ExamResultSubjectResponse> subjects) {}
