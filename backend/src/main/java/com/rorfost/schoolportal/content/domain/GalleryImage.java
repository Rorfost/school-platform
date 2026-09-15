package com.rorfost.schoolportal.content.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gallery_images")
public class GalleryImage extends AuditableUuidEntity {

  private UUID schoolId;
  private UUID galleryAlbumId;
  private String storageBucket;
  private String objectKey;
  private String originalFilename;
  private String contentType;
  private long byteSize;
  private String checksumSha256;
  private String altText;
  private String caption;
  private int sortOrder;

  @Enumerated(EnumType.STRING)
  private PublicationStatus status = PublicationStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected GalleryImage() {}

  public GalleryImage(
      UUID schoolId,
      UUID albumId,
      String bucket,
      String key,
      String filename,
      String contentType,
      long byteSize,
      String checksumSha256,
      String altText,
      String caption,
      int sortOrder) {
    this.schoolId = schoolId;
    this.galleryAlbumId = albumId;
    this.storageBucket = bucket;
    this.objectKey = key;
    this.originalFilename = filename;
    this.contentType = contentType;
    this.byteSize = byteSize;
    this.checksumSha256 = checksumSha256;
    this.altText = altText;
    this.caption = caption;
    this.sortOrder = sortOrder;
  }

  public UUID getGalleryAlbumId() {
    return galleryAlbumId;
  }

  public String getObjectKey() {
    return objectKey;
  }

  public String getStorageBucket() {
    return storageBucket;
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

  public String getAltText() {
    return altText;
  }

  public String getCaption() {
    return caption;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public PublicationStatus getStatus() {
    return status;
  }

  public void update(String altText, String caption, int sortOrder) {
    this.altText = altText;
    this.caption = caption;
    this.sortOrder = sortOrder;
  }

  public void publish(Instant at) {
    status = PublicationStatus.PUBLISHED;
    publishedAt = at;
    archivedAt = null;
  }

  public void archive(Instant at) {
    status = PublicationStatus.ARCHIVED;
    archivedAt = at;
  }
}
