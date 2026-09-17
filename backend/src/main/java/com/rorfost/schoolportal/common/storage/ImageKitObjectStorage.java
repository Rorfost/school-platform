package com.rorfost.schoolportal.common.storage;

import io.imagekit.client.ImageKitClient;
import io.imagekit.core.MultipartField;
import io.imagekit.models.assets.AssetListParams;
import io.imagekit.models.assets.AssetListResponse;
import io.imagekit.models.files.File;
import io.imagekit.models.files.FileDeleteParams;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class ImageKitObjectStorage implements ObjectStorage {
  private final ImageKitClient client;

  public ImageKitObjectStorage(ImageKitClient client) {
    this.client = client;
  }

  @Override
  public String put(
      String bucket,
      String objectKey,
      InputStream content,
      long contentLength,
      String contentType) {
    int separator = objectKey.lastIndexOf('/');
    if (separator <= 0 || separator == objectKey.length() - 1)
      throw new IllegalArgumentException("ImageKit object key must include a folder and filename");

    String folder = "/" + objectKey.substring(0, separator);
    String filename = objectKey.substring(separator + 1);
    FileUploadResponse response =
        client
            .files()
            .upload(
                FileUploadParams.builder()
                    .file(
                        MultipartField.<InputStream>builder()
                            .value(content)
                            .filename(filename)
                            .contentType(contentType)
                            .build())
                    .fileName(filename)
                    .folder(folder)
                    .useUniqueFileName(false)
                    .overwriteFile(false)
                    .build());

    String expectedPath = imageKitPath(objectKey);
    if (!expectedPath.equals(response.filePath().orElse(null))) {
      response.fileId().ifPresent(uploadedFileId -> client.files().delete(uploadedFileId));
      throw new IllegalStateException("ImageKit upload did not retain the requested object path");
    }
    return response
        .fileId()
        .orElseThrow(() -> new IllegalStateException("ImageKit upload returned no file ID"));
  }

  @Override
  public void delete(String bucket, String objectKey, String providerFileId) {
    if (providerFileId != null && !providerFileId.isBlank()) {
      client.files().delete(FileDeleteParams.builder().fileId(providerFileId).build());
      return;
    }
    // Legacy records use an exact documented filePath lookup. A missing object is already clean.
    findExactFile(objectKey)
        .flatMap(File::fileId)
        .ifPresent(
            fileId -> client.files().delete(FileDeleteParams.builder().fileId(fileId).build()));
  }

  @Override
  public boolean exists(String bucket, String objectKey) {
    return findExactFile(objectKey).isPresent();
  }

  private Optional<File> findExactFile(String objectKey) {
    String expectedPath = imageKitPath(objectKey);
    List<File> exactMatches =
        client
            .assets()
            .list(
                AssetListParams.builder()
                    .searchQuery("filePath = \"" + expectedPath + "\"")
                    .limit(2)
                    .build())
            .stream()
            .filter(AssetListResponse::isFile)
            .map(AssetListResponse::asFile)
            .filter(file -> expectedPath.equals(file.filePath().orElse(null)))
            .toList();

    if (exactMatches.size() > 1)
      throw new IllegalStateException("ImageKit returned multiple files for one object path");
    return exactMatches.stream().findFirst();
  }

  private String imageKitPath(String objectKey) {
    return "/" + objectKey;
  }
}
