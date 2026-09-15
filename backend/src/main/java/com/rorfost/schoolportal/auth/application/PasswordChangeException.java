package com.rorfost.schoolportal.auth.application;

public class PasswordChangeException extends RuntimeException {

  public PasswordChangeException() {
    super("Password change rejected");
  }
}
