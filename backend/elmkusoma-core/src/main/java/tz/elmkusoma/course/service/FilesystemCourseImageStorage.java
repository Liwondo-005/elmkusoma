package tz.elmkusoma.course.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Filesystem-backed course image storage.
 *
 * <p>The directory is configuration, not an accident of how the process was launched. The old
 * behaviour resolved to {@code ${user.dir}/uploads}, which silently pointed somewhere different
 * depending on the working directory and was not set anywhere in the project's config; pointing it
 * at persistent or shared storage is now a deployment decision rather than a filesystem race.</p>
 *
 * <p>Objects are partitioned by institution directory. That is not cosmetic: it is what makes the
 * course save path able to reject a thumbnail reference pointing at another school's upload.</p>
 */
@Slf4j
@Component
public class FilesystemCourseImageStorage implements CourseImageStorage {

    private final Path root;

    public FilesystemCourseImageStorage(
            @Value("${file.upload.dir:${user.dir}/uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir, "courses").toAbsolutePath().normalize();
        log.info("Course image filesystem storage rooted at {}", root);
    }

    @Override
    public StoredImage store(MultipartFile file, UUID institutionId, String bearerToken) {
        String extension = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        Path directory = root.resolve(institutionId.toString()).normalize();
        Path target = directory.resolve(filename).normalize();

        // Belt and braces: the filename is server-generated, but a normalisation escape here would
        // let an upload land outside the image root entirely.
        if (!target.startsWith(root)) {
            throw new StorageException("Refusing to write outside the image storage root", null);
        }

        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new StorageException("Could not store the uploaded image", e);
        }
        return new StoredImage("/v1/content/courses/" + institutionId + "/" + filename);
    }

    /**
     * Resolves a stored image to a readable file, or empty when it is absent.
     *
     * <p>Accepts a null institution because images uploaded before institution partitioning keep
     * living flat in the root. Those still resolve, so the change does not strand existing rows.</p>
     */
    public java.util.Optional<Path> resolve(UUID institutionId, String filename) {
        Path candidate = (institutionId == null ? root : root.resolve(institutionId.toString()))
                .resolve(filename)
                .normalize();
        try {
            if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
                return java.util.Optional.empty();
            }
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(candidate);
    }

    public Path getRoot() {
        return root;
    }

    private static String extensionOf(String originalName) {
        if (originalName == null) {
            return "png";
        }
        int dot = originalName.lastIndexOf('.');
        return dot >= 0 ? originalName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "png";
    }
}
