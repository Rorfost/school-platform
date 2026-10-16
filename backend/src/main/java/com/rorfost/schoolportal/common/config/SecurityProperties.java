package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security")
public record SecurityProperties(int loginMaxAttempts, Duration loginAttemptWindow) {}
