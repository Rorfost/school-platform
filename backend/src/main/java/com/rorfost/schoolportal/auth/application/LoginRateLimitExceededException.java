package com.rorfost.schoolportal.auth.application;

public class LoginRateLimitExceededException extends RuntimeException {

  public LoginRateLimitExceededException() {
    super("Login rate limit exceeded");
  }
}
