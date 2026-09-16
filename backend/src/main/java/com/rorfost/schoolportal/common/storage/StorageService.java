package com.rorfost.schoolportal.common.storage;

import com.rorfost.schoolportal.common.config.StorageProperties;
import com.rorfost.schoolportal.common.config.UploadProperties;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorageService {
  static final String IMAGEKIT_STORAGE_BUCKET = "imagekit";
  private static final Set<String> DOCUMENT_TYPES = Set.of("application/pdf");
  private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
  private final ObjectStorage storage;
  private final StorageProperties properties;
  private final UploadProperties uploads;

  public StorageService(
      ObjectStorage storage, StorageProperties properties, UploadProperties uploads) {
    this.storage = storage;
    this.properties = properties;
    this.uploads = uploads;
  }

  public StoredObject uploadPublicDocument(String prefix, MultipartFile file) {
    return upload(prefix, file, DOCUMENT_TYPES);
  }

  public StoredObject uploadPublicImage(String prefix, MultipartFile file) {
    return upload(prefix, file, IMAGE_TYPES);
  }

  public String publicUrl(String objectKey) {
    validateKey(objectKey);
    if (properties.urlEndpoint() == null || properties.urlEndpoint().isBlank())
      throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "public_storage_url_unavailable");
    return properties.urlEndpoint().replaceAll("/$", "") + "/" + objectKey;
  }

  public String publicImageThumbnailUrl(String objectKey) {
    return publicUrl(objectKey) + "?tr=w-800,h-600,c-at_max,q-80";
  }

  public void delete(StoredObject object) {
    delete(object.bucket(), object.objectKey());
  }

  public void delete(String bucket, String key) {
    validateKey(key);
    storage.delete(bucket, key);
  }

  public boolean exists(String bucket, String key) {
    validateKey(key);
    return storage.exists(bucket, key);
  }

  private StoredObject upload(String prefix, MultipartFile file, Set<String> allowedTypes) {
    if (file == null || file.isEmpty() || file.getSize() <= 0)
      throw new DomainException(HttpStatus.BAD_REQUEST, "upload_empty");
    if (file.getSize() > uploads.maxFileSize().toBytes())
      throw new DomainException(HttpStatus.PAYLOAD_TOO_LARGE, "upload_too_large");
    String type = normalizedType(file.getContentType());
    if (!allowedTypes.contains(type))
      throw new DomainException(HttpStatus.BAD_REQUEST, "upload_type_unsupported");
    String filename = safeFilename(file.getOriginalFilename());
    validateExtension(filename, type);
    try {
      byte[] bytes = file.getBytes();
      if (!hasExpectedSignature(bytes, type))
        throw new DomainException(HttpStatus.BAD_REQUEST, "upload_content_invalid");
      String key = prefix + "/" + UUID.randomUUID() + extension(type);
      validateKey(key);
      storage.put(
          IMAGEKIT_STORAGE_BUCKET,
          key,
          new java.io.ByteArrayInputStream(bytes),
          bytes.length,
          type);
      return new StoredObject(
          IMAGEKIT_STORAGE_BUCKET, key, filename, type, bytes.length, sha256(bytes));
    } catch (IOException exception) {
      throw new DomainException(HttpStatus.BAD_REQUEST, "upload_unreadable");
    }
  }

  private void validateKey(String key) {
    if (key == null || !key.matches("^[a-z0-9][a-z0-9/_.-]{0,510}$") || key.contains(".."))
      throw new DomainException(HttpStatus.BAD_REQUEST, "storage_key_invalid");
  }

  private String normalizedType(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
  }

  private String safeFilename(String value) {
    if (value == null
        || value.isBlank()
        || value.length() > 255
        || value.contains("/")
        || value.contains("\\"))
      throw new DomainException(HttpStatus.BAD_REQUEST, "upload_filename_invalid");
    return value.trim();
  }

  private void validateExtension(String filename, String type) {
    String lower = filename.toLowerCase(Locale.ROOT);
    boolean valid =
        (type.equals("application/pdf") && lower.endsWith(".pdf"))
            || (type.equals("image/jpeg") && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")))
            || (type.equals("image/png") && lower.endsWith(".png"))
            || (type.equals("image/webp") && lower.endsWith(".webp"));
    if (!valid) throw new DomainException(HttpStatus.BAD_REQUEST, "upload_extension_invalid");
  }

  private boolean hasExpectedSignature(byte[] bytes, String type) {
    if (type.equals("application/pdf"))
      return bytes.length >= 5
          && bytes[0] == '%'
          && bytes[1] == 'P'
          && bytes[2] == 'D'
          && bytes[3] == 'F'
          && bytes[4] == '-';
    if (type.equals("image/jpeg"))
      return bytes.length >= 3
          && (bytes[0] & 0xff) == 0xff
          && (bytes[1] & 0xff) == 0xd8
          && (bytes[2] & 0xff) == 0xff;
    if (type.equals("image/png"))
      return bytes.length >= 8
          && bytes[0] == (byte) 0x89
          && bytes[1] == 0x50
          && bytes[2] == 0x4e
          && bytes[3] == 0x47;
    return bytes.length >= 12
        && bytes[0] == 'R'
        && bytes[1] == 'I'
        && bytes[2] == 'F'
        && bytes[3] == 'F'
        && bytes[8] == 'W'
        && bytes[9] == 'E'
        && bytes[10] == 'B'
        && bytes[11] == 'P';
  }

  private String extension(String type) {
    return switch (type) {
      case "application/pdf" -> ".pdf";
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> throw new IllegalArgumentException("Unsupported content type");
    };
  }

  private String sha256(byte[] value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      StringBuilder builder = new StringBuilder(64);
      for (byte item : digest) builder.append(String.format("%02x", item));
      return builder.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
