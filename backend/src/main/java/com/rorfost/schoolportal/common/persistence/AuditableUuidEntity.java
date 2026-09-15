package com.rorfost.schoolportal.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
public abstract class AuditableUuidEntity extends UuidEntity {

  @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false, nullable = false)
  private Instant updatedAt;

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
