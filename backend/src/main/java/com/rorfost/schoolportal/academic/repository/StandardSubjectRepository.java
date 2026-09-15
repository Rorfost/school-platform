package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.StandardSubject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardSubjectRepository extends JpaRepository<StandardSubject, UUID> {
  List<StandardSubject> findBySchoolIdAndStandardIdOrderBySortOrder(UUID schoolId, UUID standardId);

  Optional<StandardSubject> findByIdAndSchoolId(UUID id, UUID schoolId);

  boolean existsByStandardIdAndSubjectId(UUID standardId, UUID subjectId);
}
