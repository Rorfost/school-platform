package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.StandardSubject;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardSubjectRepository extends JpaRepository<StandardSubject, UUID> {}
