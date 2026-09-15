package com.rorfost.schoolportal.content.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "study_materials")
public class StudyMaterial extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID academicYearId;
  private UUID standardSubjectId;
  private String title;
  private String description;
  private String materialType;
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

  protected StudyMaterial() {}

  public StudyMaterial(
      UUID schoolId,
      UUID academicYearId,
      UUID standardSubjectId,
      String title,
      String description,
      String materialType,
      String storageBucket,
      String objectKey,
      String originalFilename,
      String contentType,
      long byteSize,
      String checksumSha256) {
    this.schoolId = schoolId;
    this.academicYearId = academicYearId;
    this.standardSubjectId = standardSubjectId;
    this.title = title;
    this.description = description;
    this.materialType = materialType;
    this.storageBucket = storageBucket;
    this.objectKey = objectKey;
    this.originalFilename = originalFilename;
    this.contentType = contentType;
    this.byteSize = byteSize;
    this.checksumSha256 = checksumSha256;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public UUID getAcademicYearId() {
    return academicYearId;
  }

  public UUID getStandardSubjectId() {
    return standardSubjectId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getMaterialType() {
    return materialType;
  }

  public String getStorageBucket() {
    return storageBucket;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getOriginalFilename() {
    return originalFilename;
  }

  public String getContentType() {
    return contentType;
  }

  public long getByteSize() {
    return byteSize;
  }

  public PublicationStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void update(
      String title,
      String description,
      String materialType,
      UUID academicYearId,
      UUID standardSubjectId) {
    this.title = title;
    this.description = description;
    this.materialType = materialType;
    this.academicYearId = academicYearId;
    this.standardSubjectId = standardSubjectId;
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
