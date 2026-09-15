package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.StudyMaterial;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, UUID> {}
