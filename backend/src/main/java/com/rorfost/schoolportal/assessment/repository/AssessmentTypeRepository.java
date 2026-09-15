package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.AssessmentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentTypeRepository extends JpaRepository<AssessmentType, UUID> {
  Optional<AssessmentType> findByIdAndIsActiveTrue(UUID id);

  List<AssessmentType> findByIsActiveTrueOrderBySortOrder();
}
