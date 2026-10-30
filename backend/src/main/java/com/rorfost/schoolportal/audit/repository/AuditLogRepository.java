package com.rorfost.schoolportal.audit.repository;

import com.rorfost.schoolportal.audit.domain.AuditLog;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);

    /** Bounded variant – always supply a {@link Pageable} with a size limit. */
    List<AuditLog> findBySchoolId(UUID schoolId, Pageable pageable);
}
