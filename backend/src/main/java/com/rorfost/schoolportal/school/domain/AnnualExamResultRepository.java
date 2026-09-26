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

  Optional<AnnualExamResult> findBySchoolIdAndAcademicYearIdAndResultTypeAndStandardAndRollNumber(
      UUID schoolId, UUID academicYearId, String resultType, String standard, Integer rollNumber);

  @Modifying
  @Query(
      "DELETE FROM AnnualExamResult r WHERE r.schoolId = :schoolId AND r.academicYearId = :academicYearId"
          + " AND r.resultType = :resultType")
  void deleteBySchoolIdAndAcademicYearIdAndResultType(
      @Param("schoolId") UUID schoolId,
      @Param("academicYearId") UUID academicYearId,
      @Param("resultType") String resultType);
}
