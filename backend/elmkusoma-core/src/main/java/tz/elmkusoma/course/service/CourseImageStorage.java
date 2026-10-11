package tz.elmkusoma.course.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Durable storage for publicly-displayed course images.
 *
 * <p>Exists because the audit found course thumbnails written straight to
 * {@code ${user.dir}/uploads/courses} by the controller. That directory is derived from the
 * <em>process working directory</em>, not from configuration, so the bytes were:</p>
 * <ul>
 *   <li>lost whenever the application was redeployed, rebuilt or run from a different directory;</li>
 *   <li>invisible to any second application instance or any other machine;</li>
 *   <li>invisible to a container whose filesystem is ephemeral.</li>
 * </ul>
 *
 * <p>Two providers, chosen by {@code course.image.storage}:</p>
 * <ul>
 *   <li>{@code media-service} — delegates to the existing elmkusoma media service, which is backed
 *       by MinIO. This is the provider to use when more than one application instance serves the
 *       platform, or when the deployment has no persistent disk. It is the project's existing
 *       object-storage path, reused rather than replaced.</li>
 *   <li>{@code filesystem} (default) — writes to a configured directory. Durable when that
 *       directory is on persistent or shared storage, which is what a single-instance or
 *       shared-volume deployment provides.</li>
 * </ul>
 *
 * <p>Either way the value persisted on the course is a stable reference, never a temporary path,
 * a {@code blob:} URL, or Base64.</p>
 */
public interface CourseImageStorage {

    /** The reference persisted on the course, and the URL the browser will request. */
    record StoredImage(String reference) {}

    /**
     * Stores the bytes and returns the reference to persist.
     *
     * @param file          the validated upload
     * @param institutionId owning institution; providers that partition storage use it
     * @param bearerToken   caller's token, forwarded only when the provider needs it to attribute
     *                      the object to an institution server-side
     * @throws StorageException when the bytes could not be persisted; the caller must not leave a
     *                          course pointing at an image that does not exist
     */
    StoredImage store(MultipartFile file, UUID institutionId, String bearerToken);

    /** Raised for any storage failure, so callers never leak a backend path or error detail. */
    class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
