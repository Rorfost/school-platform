package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.Mark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MarkRepository extends JpaRepository<Mark, UUID> {
    List<Mark> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);

    List<Mark> findBySchoolIdAndAssessmentIdAndStudentId(
            UUID schoolId, UUID assessmentId, UUID studentId);
}
