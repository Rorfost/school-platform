package com.rorfost.schoolportal.common.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(
    URI endpoint,
    String region,
    String accessKeyId,
    String secretAccessKey,
    String publicBucket,
    String privateBucket,
    URI publicBaseUrl) {}
