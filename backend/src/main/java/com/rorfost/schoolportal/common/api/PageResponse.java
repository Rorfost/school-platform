package com.rorfost.schoolportal.common.api;

import org.springframework.data.domain.Page;

public record PageResponse<T>(
    java.util.List<T> items, int page, int size, long totalItems, int totalPages) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
