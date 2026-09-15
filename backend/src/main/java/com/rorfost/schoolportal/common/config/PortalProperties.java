package com.rorfost.schoolportal.common.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record PortalProperties(URI frontendUrl) {}
