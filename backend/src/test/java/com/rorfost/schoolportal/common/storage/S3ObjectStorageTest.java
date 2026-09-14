package com.rorfost.schoolportal.common.storage;

import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3ObjectStorageTest {

  private final S3Client client = Mockito.mock(S3Client.class);
  private final S3ObjectStorage storage = new S3ObjectStorage(client);

  @Test
  void sendsObjectMetadataToTheS3CompatibleClient() {
    storage.put(
        "school-public",
        "foundation/example.txt",
        new ByteArrayInputStream("sample".getBytes(StandardCharsets.UTF_8)),
        6,
        "text/plain");

    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(client).putObject(request.capture(), Mockito.<RequestBody>any());
    org.junit.jupiter.api.Assertions.assertEquals("school-public", request.getValue().bucket());
    org.junit.jupiter.api.Assertions.assertEquals(
        "foundation/example.txt", request.getValue().key());
  }

  @Test
  void deletesAnObjectByBucketAndKey() {
    storage.delete("school-private", "temporary/upload");

    ArgumentCaptor<DeleteObjectRequest> request =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(client).deleteObject(request.capture());
    org.junit.jupiter.api.Assertions.assertEquals("school-private", request.getValue().bucket());
    org.junit.jupiter.api.Assertions.assertEquals("temporary/upload", request.getValue().key());
  }
}
