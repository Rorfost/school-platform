package com.rorfost.schoolportal.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpectedException(Exception exception, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed.");
    problem.setTitle("Internal Server Error");
    problem.setProperty("code", "internal_error");
    problem.setProperty("requestId", MDC.get("requestId"));
    problem.setInstance(java.net.URI.create(request.getRequestURI()));
    return problem;
  }
}
