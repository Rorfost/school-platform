package com.rorfost.schoolportal.assessment.repository;

import com.rorfost.schoolportal.assessment.domain.AssessmentSubject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentSubjectRepository extends JpaRepository<AssessmentSubject, UUID> {}
