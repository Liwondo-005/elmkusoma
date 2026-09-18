package tz.elmkusoma.media.controller;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.media.dto.MediaFileResponse;
import tz.elmkusoma.media.dto.PresignedUrlResponse;
import tz.elmkusoma.media.service.MediaService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<MediaFileResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("institutionId") Long institutionId,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "metadata", required = false) String metadata) {
        log.info("Upload request for institution: {}, user: {}, fileName: {}",
                institutionId, userId, file.getOriginalFilename());

        MediaFileResponse response = mediaService.uploadMedia(institutionId, userId, file, metadata);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUrlResponse> getPresignedUploadUrl(
            @RequestParam("institutionId") Long institutionId,
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType) {
        log.info("Presigned upload URL request for institution: {}, fileName: {}", institutionId, fileName);

        PresignedUrlResponse response = mediaService.getPresignedUploadUrl(institutionId, fileName, contentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaFileResponse> getMedia(@PathVariable Long id) {
        log.info("Get media request for id: {}", id);

        MediaFileResponse response = mediaService.getMedia(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(@PathVariable Long id) {
        log.info("Download URL request for media id: {}", id);

        PresignedUrlResponse response = mediaService.getPresignedDownloadUrl(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMedia(@PathVariable Long id) {
        log.info("Delete media request for id: {}", id);

        mediaService.deleteMedia(id);
        return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<MediaFileResponse>> listMedia(
            @RequestParam("institutionId") Long institutionId,
            @RequestParam(value = "contentType", required = false) String contentType) {
        log.info("List media request for institution: {}, contentType: {}", institutionId, contentType);

        List<MediaFileResponse> responses = mediaService.listMedia(institutionId, contentType);
        return ResponseEntity.ok(responses);
    }
}
