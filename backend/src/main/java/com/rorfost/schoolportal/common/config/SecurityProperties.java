package com.rorfost.schoolportal.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security")
public record SecurityProperties(int loginMaxAttempts, Duration loginAttemptWindow) {}
