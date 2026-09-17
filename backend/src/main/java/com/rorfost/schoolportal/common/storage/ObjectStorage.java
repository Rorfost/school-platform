package com.rorfost.schoolportal.common.storage;

import java.io.InputStream;

public interface ObjectStorage {

  String put(
      String bucket, String objectKey, InputStream content, long contentLength, String contentType);

  void delete(String bucket, String objectKey, String providerFileId);

  boolean exists(String bucket, String objectKey);
}
