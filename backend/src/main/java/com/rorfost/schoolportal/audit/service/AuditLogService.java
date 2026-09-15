package com.rorfost.schoolportal.audit.service;

import com.rorfost.schoolportal.audit.domain.AuditAction;
import com.rorfost.schoolportal.audit.domain.AuditLog;
import com.rorfost.schoolportal.audit.repository.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

  private static final String EMPTY_METADATA = "{}";

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      UUID schoolId,
      UUID actorAdminUserId,
      AuditAction action,
      String targetType,
      UUID targetId,
      String requestId) {
    auditLogRepository.save(
        new AuditLog(
            schoolId, actorAdminUserId, action, targetType, targetId, requestId, EMPTY_METADATA));
  }
}
