package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Subject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
  List<Subject> findBySchoolIdOrderBySortOrder(UUID schoolId);

  Optional<Subject> findByIdAndSchoolId(UUID id, UUID schoolId);

  long countBySchoolIdAndIsArchivedFalse(UUID schoolId);
}
