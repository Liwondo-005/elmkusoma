package tz.elmkusoma.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.liveclass.service.MediaProxyService;

import java.util.Map;
import java.util.UUID;

/**
 * Course image storage backed by the existing elmkusoma media service (MinIO).
 *
 * <p>Selected with {@code course.image.storage=media-service}. This is the provider to use when
 * more than one application instance serves the platform, or when the deployment has no
 * persistent disk: the object lives in object storage, not on whichever node accepted the
 * upload, so any instance — and any later deploy — can serve it.</p>
 *
 * <p>It reuses {@link MediaProxyService} rather than opening a second HTTP client, and forwards
 * the caller's bearer token so the media service attributes the object to the institution from
 * verified claims rather than from anything the client could assert.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "course.image.storage", havingValue = "media-service")
public class MediaServiceCourseImageStorage implements CourseImageStorage {

    private final MediaProxyService mediaProxyService;

    @Override
    public StoredImage store(MultipartFile file, UUID institutionId, String bearerToken) {
        final Map<String, Object> stored;
        try {
            stored = mediaProxyService.uploadFile(file, bearerToken);
        } catch (RuntimeException e) {
            // Deliberately not rethrowing the cause: the caller must not surface the media
            // service's URL or error text to whoever uploaded the image.
            log.error("Course image upload to the media service failed: {}", e.getMessage());
            throw new StorageException("Could not store the uploaded image", e);
        }

        String url = stringOf(stored, "url");
        if (url == null || url.isBlank()) {
            url = stringOf(stored, "storageUrl");
        }
        if (url == null || url.isBlank()) {
            log.error("Media service returned no usable URL for an institution {} course image", institutionId);
            throw new StorageException("Could not store the uploaded image", null);
        }
        return new StoredImage(url);
    }

    private static String stringOf(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
