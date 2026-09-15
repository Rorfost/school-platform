package com.rorfost.schoolportal.common.storage;

public record StoredObject(
    String bucket,
    String objectKey,
    String originalFilename,
    String contentType,
    long byteSize,
    String checksumSha256) {}
