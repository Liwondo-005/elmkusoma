package tz.elmkusoma.media.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tz.elmkusoma.media.dto.MediaFileInfo;
import tz.elmkusoma.media.service.StorageService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

/**
 * Filesystem-backed implementation of the existing {@link StorageService}
 * abstraction.  Selected with {@code media.storage=local} (default remains
 * {@code minio}, so production behaviour is unchanged); it exists so the whole
 * upload → store → preview → download chain can run in environments where a
 * MinIO process is unavailable, without touching the API surface, database, or
 * any caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "media.storage", havingValue = "local")
public class LocalStorageService implements StorageService {

    @Value("${media.local.dir:${java.io.tmpdir}/elmkusoma-media}")
    private String baseDir;

    @Value("${media.local.base-url:http://localhost:8083}")
    private String baseUrl;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public String uploadFile(InputStream inputStream, String objectName, String contentType, long size) {
        Path target = resolveSafe(objectName);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored local object: {} ({} bytes)", objectName, Files.size(target));
            return plainUrl(objectName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file locally", e);
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        Path target = resolveSafe(objectName);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read stored file", e);
        }
    }

    @Override
    public String getPresignedUploadUrl(String objectName, String contentType, int expirationMinutes) {
        long exp = Instant.now().getEpochSecond() + expirationMinutes * 60L;
        String sig = LocalMediaUrls.sign(objectName, exp, jwtSecret);
        return baseUrl + "/api/v1/media/local-upload?key=" + LocalMediaUrls.encodeKey(objectName)
                + "&exp=" + exp + "&sig=" + sig;
    }

    @Override
    public String getPresignedDownloadUrl(String objectName, int expirationMinutes) {
        long exp = Instant.now().getEpochSecond() + expirationMinutes * 60L;
        String sig = LocalMediaUrls.sign(objectName, exp, jwtSecret);
        return baseUrl + "/api/v1/media/local-content?key=" + LocalMediaUrls.encodeKey(objectName)
                + "&exp=" + exp + "&sig=" + sig;
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            Files.deleteIfExists(resolveSafe(objectName));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete stored file", e);
        }
    }

    @Override
    public MediaFileInfo getFileInfo(String objectName) {
        Path target = resolveSafe(objectName);
        try {
            return MediaFileInfo.builder()
                    .objectName(objectName)
                    .contentType(Files.probeContentType(target))
                    .size(Files.size(target))
                    .lastModified(Files.getLastModifiedTime(target).toInstant())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to stat stored file", e);
        }
    }

    /** Long-lived (unsigned) reference stored alongside the media row. */
    private String plainUrl(String objectName) {
        return baseUrl + "/api/v1/media/local-content?key=" + LocalMediaUrls.encodeKey(objectName);
    }

    /**
     * Resolves an object key under the store root and rejects anything that
     * would escape it (path traversal via "..", absolute paths, backslashes).
     */
    private Path resolveSafe(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new SecurityException("Object key is required");
        }
        String normalized = objectName.replace('\\', '/');
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(normalized).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Invalid object key");
        }
        return resolved;
    }

    /** Used by the local content/upload controller for streaming. */
    public Path resolveForController(String objectName) {
        return resolveSafe(objectName);
    }

    public String signExpiry(String objectKey, long exp) {
        return LocalMediaUrls.sign(objectKey, exp, jwtSecret);
    }

    public boolean verifySignature(String objectKey, long exp, String sig) {
        return LocalMediaUrls.verify(objectKey, exp, sig, jwtSecret);
    }

    /** Writes a presigned upload body to disk (controller helper). */
    public void writePresignedUpload(String objectName, InputStream body) {
        Path target = resolveSafe(objectName);
        try {
            Files.createDirectories(target.getParent());
            try (OutputStream out = Files.newOutputStream(target)) {
                body.transferTo(out);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to store presigned upload", e);
        }
    }
}
