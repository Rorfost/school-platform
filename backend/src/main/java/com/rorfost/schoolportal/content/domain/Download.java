package com.rorfost.schoolportal.content.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "downloads")
public class Download extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private String title;
  private String description;
  private String category;
  private String storageBucket;
  private String objectKey;
  private String originalFilename;
  private String contentType;
  private long byteSize;
  private String checksumSha256;

  @Enumerated(EnumType.STRING)
  private PublicationStatus status = PublicationStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected Download() {}
}
