package tz.elmkusoma.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.service.CourseImageStorage;
import tz.elmkusoma.course.service.FilesystemCourseImageStorage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Course image upload and public serving.
 *
 * <p>Upload is Institution Admin / Platform Admin only and is institution-scoped: the bytes land
 * under a directory named for the caller's institution, and the returned reference carries that
 * institution so {@code CourseService} can refuse a thumbnail pointing at another school's upload.
 * Without that, {@code thumbnailUrl} was an arbitrary client-supplied string that any course
 * author could point anywhere.</p>
 *
 * <p>Content is checked by magic bytes, not by filename or declared MIME type. The previous
 * implementation trusted both, and served the result from an anonymous {@code /v1/content/**}
 * route — so an HTML file named {@code .png} was accepted and then handed back as
 * {@code image/png} to any visitor. This matches the rule already applied to news and live-class
 * covers.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
@Tag(name = "Course Thumbnails", description = "Course image upload and public file serving")
public class CourseThumbnailController {

    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** Server-generated name: a UUID plus an allow-listed extension. Never a client path. */
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp|gif)$");

    private static final Pattern SAFE_INSTITUTION = Pattern.compile("^[0-9a-fA-F-]{36}$");

    private final CourseImageStorage storage;
    private final FilesystemCourseImageStorage filesystemStorage;

    @Value("${course.image.storage:filesystem}")
    private String activeProvider;

    // ── upload ────────────────────────────────────────────────────────────────────────────

    @PostMapping("/courses/thumbnail")
    @Operation(summary = "Upload a course thumbnail image",
            description = "Stores the image durably and returns the reference to persist as the course thumbnail")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String rejection = validate(file);
        if (rejection != null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(rejection));
        }

        try {
            CourseImageStorage.StoredImage stored = storage.store(file, institutionId, authorization);
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("url", stored.reference());
            // Surfaced so the admin can tell which backend actually holds the bytes. Without it,
            // "it saved" reads the same whether the object is in MinIO or in a container's
            // ephemeral filesystem.
            payload.put("storage", activeProvider);
            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", payload));
        } catch (CourseImageStorage.StorageException e) {
            // No path, no upstream detail: the caller learns only that it did not save.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("The image could not be stored. Please try again."));
        }
    }

    // ── public serving ────────────────────────────────────────────────────────────────────

    @GetMapping("/content/courses/{filename}")
    @Operation(summary = "Serve a course image",
            description = "Public, read-only serving for images uploaded before institution partitioning")
    public ResponseEntity<?> getLegacyCourseImage(@PathVariable String filename) {
        return serve(null, filename);
    }

    @GetMapping("/content/courses/{institutionId}/{filename}")
    @Operation(summary = "Serve a course image",
            description = "Public, read-only serving for institution-partitioned course images")
    public ResponseEntity<?> getCourseImage(@PathVariable String institutionId, @PathVariable String filename) {
        if (institutionId == null || !SAFE_INSTITUTION.matcher(institutionId).matches()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid image name"));
        }
        return serve(UUID.fromString(institutionId), filename);
    }

    private ResponseEntity<?> serve(UUID institutionId, String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid image name"));
        }

        Optional<Path> path = filesystemStorage.resolve(institutionId, filename);
        if (path.isEmpty()) {
            // The bytes may legitimately live in object storage instead, in which case this
            // service never has them. That is not an error the visitor needs to see.
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(mediaTypeFor(filename))
                // Serving arbitrary bytes from an anonymous route without nosniff is how a file
                // that is really HTML or SVG gets its declared type honoured by the browser.
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .body(new org.springframework.core.io.FileSystemResource(path.get()));
    }

    // ── validation ────────────────────────────────────────────────────────────────────────

    /**
     * Returns a rejection reason, or null when the upload is acceptable.
     *
     * <p>Extension and declared type are checked first because they are cheap, but the decisive
     * check is the signature: a file called {@code cover.png} that is really HTML passes both
     * cheap checks and is caught here.</p>
     */
    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return "No file provided";
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return "File exceeds the 5 MB limit";
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 ? originalName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "Only jpg, jpeg, png, webp, and gif images are allowed";
        }

        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!declared.startsWith("image/")) {
            return "File must be an image";
        }
        // image/svg+xml is an image/* type that can carry script. It is not in the extension
        // allow-list, and saying so explicitly is cheaper than discovering it later.
        if (declared.contains("svg")) {
            return "SVG images are not accepted";
        }

        if (!hasImageSignature(file, extension)) {
            return "The file content is not a valid image";
        }
        return null;
    }

    private boolean hasImageSignature(MultipartFile file, String extension) {
        byte[] head = new byte[12];
        int read;
        try (var in = file.getInputStream()) {
            read = in.read(head);
        } catch (Exception e) {
            return false;
        }
        if (read < 4) {
            return false;
        }

        byte[] b = head;
        boolean jpeg = (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
        boolean png = read >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean gif = read >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F';
        boolean webp = read >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        boolean jpeg2000 = read >= 12 && b[0] == 0x00 && b[1] == 0x00 && b[2] == 0x00 && b[3] == 0x0C
                && b[4] == 'j' && b[5] == 'P';

        return switch (extension) {
            case "jpg", "jpeg" -> jpeg;
            case "png" -> png;
            case "gif" -> gif;
            case "webp" -> webp;
            // jp2 is a real JPEG 2000 image; accept only when the signature agrees.
            default -> jpeg2000 || png || jpeg || gif || webp;
        };
    }

    private static MediaType mediaTypeFor(String filename) {
        return switch (filename.substring(filename.lastIndexOf('.') + 1)) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
    }

    /** True when a persisted thumbnail reference points at this platform's own image space. */
    public static boolean isPlatformImageReference(String reference) {
        return reference != null && reference.startsWith("/v1/content/courses/");
    }

    /** The institution segment of a platform image reference, or null when it has none. */
    public static UUID institutionOf(String reference) {
        if (!isPlatformImageReference(reference)) {
            return null;
        }
        String rest = reference.substring("/v1/content/courses/".length());
        int slash = rest.indexOf('/');
        String candidate = slash >= 0 ? rest.substring(0, slash) : "";
        try {
            return SAFE_INSTITUTION.matcher(candidate).matches() ? UUID.fromString(candidate) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
