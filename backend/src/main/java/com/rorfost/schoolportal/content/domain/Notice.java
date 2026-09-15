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
  private boolean isPinned;
  private Instant expiresAt;

  @Enumerated(EnumType.STRING)
  private PublicationStatus status = PublicationStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected Notice() {}

  public Notice(UUID schoolId, String title, String body) {
    this.schoolId = schoolId;
    this.title = title;
    this.body = body;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getAttachmentBucket() {
    return attachmentBucket;
  }

  public String getAttachmentObjectKey() {
    return attachmentObjectKey;
  }

  public String getAttachmentFilename() {
    return attachmentFilename;
  }

  public String getAttachmentContentType() {
    return attachmentContentType;
  }

  public Long getAttachmentByteSize() {
    return attachmentByteSize;
  }

  public boolean isPinned() {
    return isPinned;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public PublicationStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void update(String title, String body, boolean pinned, Instant expiresAt) {
    this.title = title;
    this.body = body;
    this.isPinned = pinned;
    this.expiresAt = expiresAt;
  }

  public void setAttachment(
      String bucket, String key, String filename, String contentType, long byteSize) {
    attachmentBucket = bucket;
    attachmentObjectKey = key;
    attachmentFilename = filename;
    attachmentContentType = contentType;
    attachmentByteSize = byteSize;
  }

  public void publish(Instant at) {
    status = PublicationStatus.PUBLISHED;
    publishedAt = at;
    archivedAt = null;
  }

  public void unpublish() {
    status = PublicationStatus.DRAFT;
    publishedAt = null;
    archivedAt = null;
  }

  public void archive(Instant at) {
    status = PublicationStatus.ARCHIVED;
    archivedAt = at;
  }
}
