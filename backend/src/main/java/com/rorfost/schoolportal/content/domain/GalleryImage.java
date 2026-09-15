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
}
