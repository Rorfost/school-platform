package com.rorfost.schoolportal.audit.repository;

import com.rorfost.schoolportal.audit.domain.AuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
}
