package com.rorfost.schoolportal.academic.repository;

import com.rorfost.schoolportal.academic.domain.Standard;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandardRepository extends JpaRepository<Standard, UUID> {}
