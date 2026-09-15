package com.rorfost.schoolportal.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

@Component
public class SecurityProblemResponder {

  private final ObjectMapper objectMapper;

  public SecurityProblemResponder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public void unauthenticated(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    write(response, request, HttpStatus.UNAUTHORIZED, "unauthenticated");
  }

  public void forbidden(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException {
    String code =
        exception instanceof InvalidCsrfTokenException
                || exception instanceof MissingCsrfTokenException
            ? "csrf_invalid"
            : "forbidden";
    write(response, request, HttpStatus.FORBIDDEN, code);
  }

  private void write(
      HttpServletResponse response, HttpServletRequest request, HttpStatus status, String code)
      throws IOException {
    Map<String, Object> problem = new LinkedHashMap<>();
    problem.put("type", "about:blank");
    problem.put("title", status.getReasonPhrase());
    problem.put("status", status.value());
    problem.put("detail", "Request could not be completed.");
    problem.put("instance", URI.create(request.getRequestURI()));
    problem.put("code", code);
    problem.put("requestId", MDC.get("requestId"));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
