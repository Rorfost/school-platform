package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.StandardSubject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StandardSubjectRepository extends JpaRepository<StandardSubject, UUID> {
  List<StandardSubject> findBySchoolIdAndStandardIdOrderBySortOrder(UUID schoolId, UUID standardId);

  List<StandardSubject> findBySchoolIdAndSubjectId(UUID schoolId, UUID subjectId);

  Optional<StandardSubject> findByIdAndSchoolId(UUID id, UUID schoolId);

  boolean existsByStandardIdAndSubjectId(UUID standardId, UUID subjectId);

  boolean existsBySubjectId(UUID subjectId);

  @Query(
      value =
          """
          SELECT EXISTS (SELECT 1 FROM study_materials WHERE standard_subject_id = :mappingId)
              OR EXISTS (SELECT 1 FROM assessment_subjects WHERE standard_subject_id = :mappingId)
          """,
      nativeQuery = true)
  boolean isReferenced(@Param("mappingId") UUID mappingId);
}
