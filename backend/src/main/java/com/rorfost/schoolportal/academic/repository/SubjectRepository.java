package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Subject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {}
