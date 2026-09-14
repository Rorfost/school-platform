package com.rorfost.schoolportal.common.storage;

import com.rorfost.schoolportal.common.config.StorageProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class StorageConfiguration {

  @Bean(destroyMethod = "close")
  S3Client s3Client(StorageProperties properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        properties.accessKeyId(), properties.secretAccessKey())));

    URI endpoint = properties.endpoint();
    if (endpoint != null) {
      builder.endpointOverride(endpoint);
    }

    return builder.build();
  }

  @Bean
  ObjectStorage objectStorage(S3Client client) {
    return new S3ObjectStorage(client);
  }
}
