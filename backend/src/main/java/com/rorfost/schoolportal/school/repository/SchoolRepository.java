package com.rorfost.schoolportal.school.repository;

import com.rorfost.schoolportal.school.domain.School;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, UUID> {
  Optional<School> findBySlug(String slug);
}
