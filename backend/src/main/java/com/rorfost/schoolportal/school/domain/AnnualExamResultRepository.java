package com.rorfost.schoolportal.school.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnualExamResultRepository extends JpaRepository<AnnualExamResult, UUID> {
  Optional<AnnualExamResult> findBySchoolIdAndAcademicYearIdAndStandardAndRollNumber(
      UUID schoolId, UUID academicYearId, String standard, Integer rollNumber);
      
  void deleteBySchoolIdAndAcademicYearId(UUID schoolId, UUID academicYearId);
}
