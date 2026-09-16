package com.rorfost.schoolportal.common.storage;

import com.rorfost.schoolportal.auth.domain.PrincipalSession;
import com.rorfost.schoolportal.common.config.StorageProperties;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/storage")
public class StorageAdminController {
  private final StorageProperties storageProperties;

  public StorageAdminController(StorageProperties storageProperties) {
    this.storageProperties = storageProperties;
  }

  @GetMapping("/upload-auth")
  public ImageKitUploadAuthResponse getUploadAuth(
      @AuthenticationPrincipal PrincipalSession principal) {
    long expire = (System.currentTimeMillis() / 1000) + 1800;
    String token = UUID.randomUUID().toString();
    String signature = "stub-signature-" + token;
    return new ImageKitUploadAuthResponse(
        token, expire, signature, storageProperties.urlEndpoint());
  }
}
