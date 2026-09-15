package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.admin-bootstrap")
public record AdminBootstrapProperties(
    String initialEmail, String initialPassword, String schoolSlug) {

  public boolean isConfigured() {
    return !isBlank(initialEmail) && !isBlank(initialPassword) && !isBlank(schoolSlug);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
