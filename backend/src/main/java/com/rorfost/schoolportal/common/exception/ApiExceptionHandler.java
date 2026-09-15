package com.rorfost.schoolportal.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidationException(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    return problem(HttpStatus.BAD_REQUEST, "validation_failed", request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail handleMissingResource(
      NoResourceFoundException exception, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, "resource_not_found", request);
  }

  @ExceptionHandler(com.rorfost.schoolportal.auth.application.AuthenticationFailedException.class)
  ProblemDetail handleAuthenticationFailure(
      RuntimeException exception, HttpServletRequest request) {
    return problem(HttpStatus.UNAUTHORIZED, "authentication_failed", request);
  }

  @ExceptionHandler(com.rorfost.schoolportal.auth.application.PasswordChangeException.class)
  ProblemDetail handlePasswordChangeFailure(
      RuntimeException exception, HttpServletRequest request) {
    return problem(HttpStatus.BAD_REQUEST, "password_change_rejected", request);
  }

  @ExceptionHandler(com.rorfost.schoolportal.auth.application.LoginRateLimitExceededException.class)
  ProblemDetail handleRateLimit(RuntimeException exception, HttpServletRequest request) {
    return problem(HttpStatus.TOO_MANY_REQUESTS, "login_rate_limited", request);
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail handleDomainException(DomainException exception, HttpServletRequest request) {
    return problem(exception.getStatus(), exception.getCode(), request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", request);
  }

  private ProblemDetail problem(HttpStatus status, String code, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(status, "Request could not be completed.");
    problem.setTitle(status.getReasonPhrase());
    problem.setProperty("code", code);
    problem.setProperty("requestId", MDC.get("requestId"));
    problem.setInstance(java.net.URI.create(request.getRequestURI()));
    return problem;
  }
}
