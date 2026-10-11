package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Cover images for public news articles.
 *
 * <p>Modelled on {@code CourseThumbnailController}, which is the established pattern in this
 * codebase, with two deliberate differences.</p>
 *
 * <p>First, this endpoint is Platform Admin only. The existing media upload is scoped to
 * TEACHER and INSTITUTION_ADMIN, which meant a Platform Admin - the only role allowed to publish
 * news - could not upload the cover for the article they are publishing.</p>
 *
 * <p>Second, and more important: it checks the file's actual bytes rather than trusting its name
 * and declared content type. A file called {@code cover.jpg} that declares {@code image/jpeg} but
 * contains HTML passes an extension-and-header check. Serving it back is safe here only because
 * the response is pinned to an image media type, but that is a single mistake away from being a
 * stored-XSS host, so the signature is verified before anything touches the disk.</p>
 *
 * <p>SVG is deliberately excluded. It is an image format that can carry script, and it would be
 * the one entry in an image allowlist that could execute in a browser.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "News Cover Images", description = "Upload and public serving of news article cover images")
public class NewsCoverController {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** A generated UUID plus a known extension. Nothing else can ever be requested. */
    private static final Pattern SAFE_FILENAME =
            Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp|gif)$");

    private static final String UPLOAD_SUBDIR = "news-covers";
    private static final String PUBLIC_PREFIX = "/v1/content/news-covers/";

    @Value("${file.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    @PostMapping("/v1/platform-admin/news/cover")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a news cover image",
            description = "Stores the image under uploads/news-covers and returns its public URL")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No file provided"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cover image exceeds the 5 MB limit"));
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 ? originalName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Only jpg, jpeg, png, webp and gif images are allowed"));
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File must be an image"));
        }

        byte[] header;
        try {
            header = new byte[12];
            try (var in = file.getInputStream()) {
                int read = in.readNBytes(header, 0, header.length);
                if (read < 4) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("File is not a readable image"));
                }
            }
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File could not be read"));
        }

        if (!matchesSignature(extension, header)) {
            // The name and the declared type both claimed image; the bytes disagree. Refusing is
            // the whole point of checking, so this is an error rather than a warning.
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "File content does not match a supported image format"));
        }

        try {
            Path directory = Paths.get(uploadDir, UPLOAD_SUBDIR).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "." + extension;
            file.transferTo(directory.resolve(filename));

            Map<String, String> body = new LinkedHashMap<>();
            body.put("url", PUBLIC_PREFIX + filename);
            body.put("filename", filename);
            return ResponseEntity.ok(ApiResponse.success("Cover image uploaded", body));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to store the uploaded image"));
        }
    }

    @GetMapping("/v1/content/news-covers/{filename}")
    @Operation(summary = "Serve a news cover image",
            description = "Public, read-only file serving for news cover images")
    public ResponseEntity<?> serve(@PathVariable String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid image name"));
        }

        Path directory = Paths.get(uploadDir, UPLOAD_SUBDIR).toAbsolutePath().normalize();
        Path path = directory.resolve(filename).normalize();
        // The regex already restricts the name, but the resolve-and-compare is kept as a second
        // line of defence in case the pattern is ever loosened.
        if (!path.startsWith(directory) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = switch (filename.substring(filename.lastIndexOf('.') + 1)) {
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            case "gif" -> MediaType.IMAGE_GIF;
            default -> MediaType.IMAGE_JPEG;
        };

        return ResponseEntity.ok()
                .contentType(mediaType)
                // Pinned explicitly: without it some clients may content-negotiate, and a file
                // that somehow slipped through as text must still not be interpreted as HTML.
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; sandbox")
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(new org.springframework.core.io.FileSystemResource(path));
    }

    /**
     * Verifies the magic number against the declared extension.
     *
     * <p>Only checks the declared type matches one of the allowed formats, and does not attempt
     * full image decoding: an attacker with a polyglot JPEG/HTML file still gets a response pinned
     * to {@code image/jpeg} with {@code nosniff}, which a browser will not execute.</p>
     */
    private static boolean matchesSignature(String extension, byte[] header) {
        return switch (extension) {
            case "jpg", "jpeg" -> header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF;
            case "png" -> header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E
                    && header[3] == 0x47 && header[4] == 0x0D && header[5] == 0x0A
                    && header[6] == 0x1A && header[7] == 0x0A;
            case "gif" -> (header[0] == 'G' && header[1] == 'I' && header[2] == 'F'
                    && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a');
            case "webp" -> header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}