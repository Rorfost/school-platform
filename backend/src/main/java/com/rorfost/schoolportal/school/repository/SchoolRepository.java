package com.rorfost.schoolportal.school.repository;

import com.rorfost.schoolportal.school.domain.School;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findBySlug(String slug);

    Optional<School> findFirstByOrderByCreatedAtAsc();

    Optional<School> findFirstByIsActiveTrueOrderByCreatedAtAsc();
}
