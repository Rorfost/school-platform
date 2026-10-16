package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.AssessmentSubject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssessmentSubjectRepository extends JpaRepository<AssessmentSubject, UUID> {
    List<AssessmentSubject> findByAssessmentIdOrderByCreatedAt(UUID assessmentId);

    boolean existsByAssessmentIdAndStandardSubjectId(UUID assessmentId, UUID standardSubjectId);
}
