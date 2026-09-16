package com.rorfost.schoolportal.common.storage;

public record ImageKitUploadAuthResponse(
    String token, long expire, String signature, String urlEndpoint) {}
