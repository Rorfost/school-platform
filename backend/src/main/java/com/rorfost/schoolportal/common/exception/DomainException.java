package com.rorfost.schoolportal.common.exception;

import org.springframework.http.HttpStatus;

public class DomainException extends RuntimeException {
  private final HttpStatus status;
  private final String code;

  public DomainException(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }
}
