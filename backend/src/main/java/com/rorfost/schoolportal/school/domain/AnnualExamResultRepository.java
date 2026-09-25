package com.rorfost.schoolportal.school.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualExamResultRepository extends JpaRepository<AnnualExamResult, UUID> {

  Optional<AnnualExamResult> findBySchoolIdAndAcademicYearIdAndStandardAndRollNumber(
      UUID schoolId, UUID academicYearId, String standard, Integer rollNumber);

  List<AnnualExamResult> findBySchoolIdAndAcademicYearId(UUID schoolId, UUID academicYearId);

  @Modifying
  @Query(
      "DELETE FROM AnnualExamResult r WHERE r.schoolId = :schoolId AND r.academicYearId ="
          + " :academicYearId")
  void deleteBySchoolIdAndAcademicYearId(
      @Param("schoolId") UUID schoolId, @Param("academicYearId") UUID academicYearId);
}
