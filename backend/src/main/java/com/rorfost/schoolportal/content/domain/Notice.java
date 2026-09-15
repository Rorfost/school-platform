package com.rorfost.schoolportal.content.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notices")
public class Notice extends AuditableUuidEntity {

  private UUID schoolId;
  private String title;
  private String body;
  private String attachmentBucket;
  private String attachmentObjectKey;
  private String attachmentFilename;
  private String attachmentContentType;
  private Long attachmentByteSize;

  @Enumerated(EnumType.STRING)
  private PublicationStatus status = PublicationStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected Notice() {}
}
