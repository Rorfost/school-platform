package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(String urlEndpoint) {}
