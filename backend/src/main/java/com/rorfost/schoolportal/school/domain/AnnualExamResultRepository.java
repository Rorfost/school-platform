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

  @Modifying
  @Query(
      "DELETE FROM AnnualExamResult r WHERE r.schoolId = :schoolId AND r.resultType = :resultType")
  void deleteBySchoolIdAndResultType(
      @Param("schoolId") UUID schoolId, @Param("resultType") String resultType);
}
