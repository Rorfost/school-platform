package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
  Optional<AcademicYear> findBySchoolIdAndStatus(UUID schoolId, AcademicYearStatus status);
}
