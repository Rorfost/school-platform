package com.rorfost.schoolportal.school.repository;

import com.rorfost.schoolportal.school.domain.PrincipalProfile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PrincipalProfileRepository extends JpaRepository<PrincipalProfile, UUID> {
    Optional<PrincipalProfile> findBySchoolId(UUID schoolId);
}
