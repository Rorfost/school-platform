package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.PublicationStatus;
import com.rorfost.schoolportal.content.domain.StudyMaterial;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, UUID> {
  Page<StudyMaterial> findBySchoolIdAndStatus(
      UUID schoolId, PublicationStatus status, Pageable pageable);

  Page<StudyMaterial> findBySchoolId(UUID schoolId, Pageable pageable);

  Page<StudyMaterial> findBySchoolIdAndStatusAndAcademicYearIdAndStandardSubjectId(
      UUID schoolId,
      PublicationStatus status,
      UUID academicYearId,
      UUID standardSubjectId,
      Pageable pageable);

  java.util.Optional<StudyMaterial> findByIdAndSchoolId(UUID id, UUID schoolId);

  long countBySchoolIdAndStatus(UUID schoolId, PublicationStatus status);
}
