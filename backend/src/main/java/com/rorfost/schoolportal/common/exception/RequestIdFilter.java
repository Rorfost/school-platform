package com.rorfost.schoolportal.common.exception;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
  private static final String HEADER_NAME = "X-Request-Id";
  private static final Logger logger = LoggerFactory.getLogger(RequestIdFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = request.getHeader(HEADER_NAME);
    if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
      requestId = UUID.randomUUID().toString();
    }

    long startedAt = System.nanoTime();
    MDC.put("requestId", requestId);
    response.setHeader(HEADER_NAME, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
      logger.info(
          "request_complete requestId={} method={} route={} status={} durationMs={}",
          requestId,
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMillis);
      MDC.remove("requestId");
    }
  }
}
