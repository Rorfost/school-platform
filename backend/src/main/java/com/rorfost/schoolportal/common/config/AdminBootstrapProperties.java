package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.admin-bootstrap")
public record AdminBootstrapProperties(boolean enabled, String email, String password) {}
