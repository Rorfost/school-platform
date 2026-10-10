package com.rorfost.schoolportal.school.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualExamResultRepository extends JpaRepository<AnnualExamResult, UUID> {

  Optional<AnnualExamResult> findBySchoolIdAndResultTypeAndStandardAndRollNumber(
      UUID schoolId, String resultType, String standard, Integer rollNumber);

  @Query(
      """
      SELECT r FROM AnnualExamResult r
      WHERE r.schoolId = :schoolId
        AND r.resultType = :resultType
        AND (LOWER(TRIM(r.standard)) = LOWER(TRIM(:standard)))
        AND (
          (:studentClass IS NULL AND (r.studentClass IS NULL OR TRIM(r.studentClass) = ''))
          OR (:studentClass IS NOT NULL AND LOWER(TRIM(r.studentClass)) = LOWER(TRIM(:studentClass)))
        )
        AND r.rollNumber = :rollNumber
      """)
  Optional<AnnualExamResult> findResult(
      @Param("schoolId") UUID schoolId,
      @Param("resultType") String resultType,
      @Param("standard") String standard,
      @Param("studentClass") String studentClass,
      @Param("rollNumber") Integer rollNumber);

  @Modifying
  @Query(
      "DELETE FROM AnnualExamResult r WHERE r.schoolId = :schoolId AND r.resultType = :resultType")
  void deleteBySchoolIdAndResultType(
      @Param("schoolId") UUID schoolId, @Param("resultType") String resultType);
}
