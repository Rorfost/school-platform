package com.rorfost.schoolportal.content.api;

import com.rorfost.schoolportal.content.domain.Notice;
import java.time.Instant;
import java.util.UUID;

public record NoticeResponse(
    UUID id,
    String title,
    String body,
    boolean pinned,
    Instant expiresAt,
    String attachmentFilename,
    String attachmentUrl,
    String status) {
  public static NoticeResponse from(Notice item, String url) {
    return new NoticeResponse(
        item.getId(),
        item.getTitle(),
        item.getBody(),
        item.isPinned(),
        item.getExpiresAt(),
        item.getAttachmentFilename(),
        url,
        item.getStatus().name());
  }
}
