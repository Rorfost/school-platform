package com.rorfost.schoolportal.auth.application;

public class AuthenticationFailedException extends RuntimeException {

  public AuthenticationFailedException() {
    super("Authentication failed");
  }
}
