package com.rorfost.schoolportal.common.storage;

import java.io.InputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ObjectStorage implements ObjectStorage {
  private final S3Client client;

  public S3ObjectStorage(S3Client client) {
    this.client = client;
  }

  @Override
  public void put(
      String bucket,
      String objectKey,
      InputStream content,
      long contentLength,
      String contentType) {
    PutObjectRequest request =
        PutObjectRequest.builder().bucket(bucket).key(objectKey).contentType(contentType).build();
    client.putObject(request, RequestBody.fromInputStream(content, contentLength));
  }

  @Override
  public void delete(String bucket, String objectKey) {
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
  }

  @Override
  public boolean exists(String bucket, String objectKey) {
    try {
      client.headObject(HeadObjectRequest.builder().bucket(bucket).key(objectKey).build());
      return true;
    } catch (NoSuchKeyException exception) {
      return false;
    }
  }
}
