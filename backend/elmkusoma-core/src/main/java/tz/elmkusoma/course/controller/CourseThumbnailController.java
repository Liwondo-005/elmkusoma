package tz.elmkusoma.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@Tag(name = "Course Thumbnails", description = "Course image upload and public file serving")
public class CourseThumbnailController {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp|gif)$");

    @Value("${file.upload.dir:${user.dir}/uploads}")
    private String uploadDir;

    @PostMapping("/v1/courses/thumbnail")
    @Operation(summary = "Upload a course thumbnail image", description = "Stores the image under uploads/courses and returns its public URL")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No file provided"));
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File exceeds the 5 MB limit"));
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = originalName.lastIndexOf('.');
        String extension = dot >= 0 ? originalName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Only jpg, jpeg, png, webp, and gif images are allowed"));
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File must be an image"));
        }

        try {
            Path directory = Paths.get(uploadDir, "courses").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + "." + extension;
            file.transferTo(directory.resolve(filename));
            String url = "/v1/content/courses/" + filename;
            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", Map.of("url", url)));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to store the uploaded image"));
        }
    }

    @GetMapping("/v1/content/courses/{filename}")
    @Operation(summary = "Serve a course image", description = "Public, read-only file serving for course thumbnails")
    public ResponseEntity<?> getCourseImage(@PathVariable String filename) {
        if (filename == null || !SAFE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid image name"));
        }
        Path directory = Paths.get(uploadDir, "courses").toAbsolutePath().normalize();
        Path path = directory.resolve(filename).normalize();
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
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS))
                .body(new org.springframework.core.io.FileSystemResource(path));
    }
}
