package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.StandardClass;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StandardClassRepository extends JpaRepository<StandardClass, UUID> {

  List<StandardClass> findBySchoolIdAndStandardIdOrderBySortOrder(UUID schoolId, UUID standardId);

  List<StandardClass> findBySchoolIdOrderBySortOrder(UUID schoolId);

  void deleteByStandardId(UUID standardId);

  boolean existsByStandardIdAndNameIgnoreCase(UUID standardId, String name);
}
