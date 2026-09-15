package com.rorfost.schoolportal.audit.repository;

import com.rorfost.schoolportal.audit.domain.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
  List<AuditLog> findBySchoolIdOrderByCreatedAtDesc(UUID schoolId);
}
