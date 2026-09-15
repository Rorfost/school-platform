package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.uploads")
public record UploadProperties(DataSize maxFileSize, DataSize maxRequestSize) {}
