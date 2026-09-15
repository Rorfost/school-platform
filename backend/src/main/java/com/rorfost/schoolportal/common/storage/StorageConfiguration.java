package com.rorfost.schoolportal.common.storage;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import java.io.InputStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class StorageConfiguration {

  @Bean(destroyMethod = "close")
  @Profile("!test")
  ImageKitClient imageKitClient() {
    String privateKey = System.getenv("IMAGEKIT_PRIVATE_KEY");
    if (privateKey == null || privateKey.isBlank())
      throw new IllegalStateException("IMAGEKIT_PRIVATE_KEY must be configured");
    return ImageKitOkHttpClient.builder().privateKey(privateKey).build();
  }

  @Bean
  @Profile("!test")
  ObjectStorage objectStorage(ImageKitClient client) {
    return new ImageKitObjectStorage(client);
  }

  @Bean
  @Profile("test")
  ObjectStorage testObjectStorage() {
    return new ObjectStorage() {
      @Override
      public void put(
          String bucket,
          String objectKey,
          InputStream content,
          long contentLength,
          String contentType) {}

      @Override
      public void delete(String bucket, String objectKey) {}

      @Override
      public boolean exists(String bucket, String objectKey) {
        return false;
      }
    };
  }
}
