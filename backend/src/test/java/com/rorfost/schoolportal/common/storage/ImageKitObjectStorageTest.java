package com.rorfost.schoolportal.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.imagekit.client.ImageKitClient;
import io.imagekit.models.assets.AssetListParams;
import io.imagekit.models.assets.AssetListResponse;
import io.imagekit.models.files.File;
import io.imagekit.models.files.FileDeleteParams;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import io.imagekit.services.blocking.AssetService;
import io.imagekit.services.blocking.FileService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ImageKitObjectStorageTest {
  private final ImageKitClient client = Mockito.mock(ImageKitClient.class);
  private final FileService files = Mockito.mock(FileService.class);
  private final AssetService assets = Mockito.mock(AssetService.class);
  private final ImageKitObjectStorage storage = new ImageKitObjectStorage(client);

  @BeforeEach
  void setUp() {
    when(client.files()).thenReturn(files);
    when(client.assets()).thenReturn(assets);
  }

  @Test
  void uploadsToTheExactRequestedImageKitFolderAndFilename() {
    String objectKey = "materials/school-slug/123e4567-e89b-12d3-a456-426614174000.pdf";
    when(files.upload(any(FileUploadParams.class)))
        .thenReturn(
            FileUploadResponse.builder().fileId("file-id").filePath("/" + objectKey).build());

    String fileId =
        storage.put(
            "imagekit",
            objectKey,
            new ByteArrayInputStream("%PDF-test".getBytes(StandardCharsets.UTF_8)),
            9,
            "application/pdf");

    ArgumentCaptor<FileUploadParams> request = ArgumentCaptor.forClass(FileUploadParams.class);
    verify(files).upload(request.capture());
    assertThat(request.getValue().folder()).contains("/materials/school-slug");
    assertThat(request.getValue().fileName()).isEqualTo("123e4567-e89b-12d3-a456-426614174000.pdf");
    assertThat(request.getValue().useUniqueFileName()).contains(false);
    assertThat(request.getValue().overwriteFile()).contains(false);
    assertThat(fileId).isEqualTo("file-id");
  }

  @Test
  void deletesNewMediaUsingItsPersistedImageKitFileIdWithoutSearching() {
    storage.delete("imagekit", "materials/school-slug/new.pdf", "stored-file-id");

    ArgumentCaptor<FileDeleteParams> delete = ArgumentCaptor.forClass(FileDeleteParams.class);
    verify(files).delete(delete.capture());
    assertThat(delete.getValue().fileId()).contains("stored-file-id");
    verify(assets, org.mockito.Mockito.never()).list(any(AssetListParams.class));
  }

  @Test
  void deletesOnlyTheFileResolvedFromAnExactObjectPath() {
    String objectKey = "downloads/school-slug/123e4567-e89b-12d3-a456-426614174000.pdf";
    when(assets.list(any(AssetListParams.class)))
        .thenReturn(List.of(AssetListResponse.ofFile(file("file-id", "/" + objectKey))));

    storage.delete("imagekit", objectKey, null);

    ArgumentCaptor<FileDeleteParams> delete = ArgumentCaptor.forClass(FileDeleteParams.class);
    verify(files).delete(delete.capture());
    assertThat(delete.getValue().fileId()).contains("file-id");
  }

  @Test
  void reportsExistsOnlyForAnExactObjectPath() {
    String objectKey = "gallery/school-slug/123e4567-e89b-12d3-a456-426614174000.jpg";
    when(assets.list(any(AssetListParams.class)))
        .thenReturn(
            List.of(
                AssetListResponse.ofFile(file("other-file", "/gallery/school-slug/other.jpg")),
                AssetListResponse.ofFile(file("file-id", "/" + objectKey))));

    assertThat(storage.exists("imagekit", objectKey)).isTrue();
    ArgumentCaptor<AssetListParams> search = ArgumentCaptor.forClass(AssetListParams.class);
    verify(assets).list(search.capture());
    assertThat(search.getValue().searchQuery())
        .contains("filePath = \"/gallery/school-slug/123e4567-e89b-12d3-a456-426614174000.jpg\"");
  }

  private File file(String fileId, String filePath) {
    return File.builder().fileId(fileId).filePath(filePath).build();
  }
}
