package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StandardRepository extends JpaRepository<Standard, UUID> {
  List<Standard> findBySchoolIdOrderBySortOrder(UUID schoolId);

  List<Standard> findBySchoolIdAndIsArchivedFalseOrderBySortOrder(UUID schoolId);

  Optional<Standard> findByIdAndSchoolId(UUID id, UUID schoolId);

  Optional<Standard> findFirstBySchoolIdOrderBySortOrderDesc(UUID schoolId);

  boolean existsBySchoolIdAndCode(UUID schoolId, String code);

  boolean existsBySchoolIdAndDisplayNameIgnoreCase(UUID schoolId, String displayName);

  boolean existsBySchoolIdAndDisplayNameIgnoreCaseAndIdNot(
      UUID schoolId, String displayName, UUID id);

  boolean existsBySchoolIdAndSortOrder(UUID schoolId, short sortOrder);

  @Query(
      value =
          """
          SELECT EXISTS (SELECT 1 FROM students WHERE standard_id = :standardId)
              OR EXISTS (SELECT 1 FROM assessments WHERE standard_id = :standardId)
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("standardId") UUID standardId);

  long countBySchoolIdAndIsArchivedFalse(UUID schoolId);
}
