package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardRepository extends JpaRepository<Standard, UUID> {
  List<Standard> findBySchoolIdOrderBySortOrder(UUID schoolId);

  Optional<Standard> findByIdAndSchoolId(UUID id, UUID schoolId);

  long countBySchoolIdAndIsArchivedFalse(UUID schoolId);
}
