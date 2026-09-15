package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.Mark;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarkRepository extends JpaRepository<Mark, UUID> {
  List<Mark> findByAssessmentIdAndStudentId(UUID assessmentId, UUID studentId);
}
