package com.rorfost.schoolportal.content.domain;

import com.rorfost.schoolportal.common.persistence.AuditableUuidEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gallery_albums")
public class GalleryAlbum extends AuditableUuidEntity {

  private UUID schoolId;
  private String title;
  private String description;
  private UUID coverImageId;

  @Enumerated(EnumType.STRING)
  private PublicationStatus status = PublicationStatus.DRAFT;

  private Instant publishedAt;
  private Instant archivedAt;

  protected GalleryAlbum() {}

  public GalleryAlbum(UUID schoolId, String title, String description) {
    this.schoolId = schoolId;
    this.title = title;
    this.description = description;
  }

  public UUID getSchoolId() {
    return schoolId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public UUID getCoverImageId() {
    return coverImageId;
  }

  public PublicationStatus getStatus() {
    return status;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void update(String title, String description) {
    this.title = title;
    this.description = description;
  }

  public void setCoverImageId(UUID coverImageId) {
    this.coverImageId = coverImageId;
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
