package com.rorfost.schoolportal.common.storage;

import java.io.InputStream;

public interface ObjectStorage {

  void put(
      String bucket, String objectKey, InputStream content, long contentLength, String contentType);

  void delete(String bucket, String objectKey);

  boolean exists(String bucket, String objectKey);
}
