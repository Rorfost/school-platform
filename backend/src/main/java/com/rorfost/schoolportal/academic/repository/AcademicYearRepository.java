package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Optional<AcademicYear> findBySchoolIdAndStatus(UUID schoolId, AcademicYearStatus status);

    List<AcademicYear> findBySchoolIdOrderByStartsOnDesc(UUID schoolId);

    boolean existsBySchoolIdAndName(UUID schoolId, String name);
}
