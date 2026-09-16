package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Student;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, UUID> {
  Optional<Student> findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
      UUID schoolId, UUID academicYearId, UUID standardId, String rollNumber);

  List<Student> findBySchoolIdAndStandardId(UUID schoolId, UUID standardId);

  List<Student> findBySchoolIdAndAcademicYearIdAndStandardId(
      UUID schoolId, UUID academicYearId, UUID standardId);

  long countBySchoolIdAndIsArchivedFalse(UUID schoolId);
}
