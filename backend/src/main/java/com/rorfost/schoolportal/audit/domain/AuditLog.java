package com.rorfost.schoolportal.audit.domain;

import com.rorfost.schoolportal.common.persistence.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends UuidEntity {

  private UUID schoolId;
  private UUID actorAdminUserId;
  private String action;
  private String targetType;
  private UUID targetId;
  private String requestId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String metadata;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  protected AuditLog() {}

  public AuditLog(
      UUID schoolId,
      UUID actorAdminUserId,
      AuditAction action,
      String targetType,
      UUID targetId,
      String requestId,
      String metadata) {
    this.schoolId = schoolId;
    this.actorAdminUserId = actorAdminUserId;
    this.action = action.name();
    this.targetType = targetType;
    this.targetId = targetId;
    this.requestId = requestId;
    this.metadata = metadata;
  }
}
