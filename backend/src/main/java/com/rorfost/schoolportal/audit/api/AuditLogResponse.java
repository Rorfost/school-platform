package com.rorfost.schoolportal.audit.api;

import com.rorfost.schoolportal.audit.domain.AuditLog;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    UUID schoolId,
    UUID actorAdminUserId,
    String action,
    String targetType,
    UUID targetId,
    String requestId,
    String metadata,
    Instant createdAt) {

  public static AuditLogResponse from(AuditLog log) {
    return new AuditLogResponse(
        log.getId(),
        log.getSchoolId(),
        log.getActorAdminUserId(),
        log.getAction(),
        log.getTargetType(),
        log.getTargetId(),
        log.getRequestId(),
        log.getMetadata(),
        log.getCreatedAt());
  }
}
