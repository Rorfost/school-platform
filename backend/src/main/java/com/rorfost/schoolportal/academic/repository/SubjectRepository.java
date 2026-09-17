package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Subject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
  List<Subject> findBySchoolIdOrderBySortOrder(UUID schoolId);

  List<Subject> findBySchoolIdAndIsArchivedFalseOrderBySortOrder(UUID schoolId);

  Optional<Subject> findByIdAndSchoolId(UUID id, UUID schoolId);

  boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);

  long countBySchoolIdAndIsArchivedFalse(UUID schoolId);
}
