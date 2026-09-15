package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.Assessment;
import com.rorfost.schoolportal.assessment.domain.AssessmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
  List<Assessment> findBySchoolIdAndAcademicYearIdAndStandardIdAndStatusOrderByPublishedAtDesc(
      UUID schoolId, UUID academicYearId, UUID standardId, AssessmentStatus status);

  List<Assessment> findBySchoolIdOrderByUpdatedAtDesc(UUID schoolId);

  Optional<Assessment> findByIdAndSchoolId(UUID id, UUID schoolId);
}
