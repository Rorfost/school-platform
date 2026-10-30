package com.rorfost.schoolportal.tenant.repository;

import com.rorfost.schoolportal.tenant.domain.SchoolDomain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolDomainRepository extends JpaRepository<SchoolDomain, UUID> {
    Optional<SchoolDomain> findByHostname(String hostname);

    List<SchoolDomain> findAllBySchoolId(UUID schoolId);

    boolean existsByHostname(String hostname);
}
