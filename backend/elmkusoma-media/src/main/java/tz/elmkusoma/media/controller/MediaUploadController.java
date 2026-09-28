package tz.elmkusoma.media.controller;

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
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaUploadController {

    private final MediaService mediaService;

    // --- identity is bound from the verified JWT only ---------------------

    @PostMapping("/upload")
    public ResponseEntity<MediaFileResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "metadata", required = false) String metadata,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        log.info("Upload request for institution: {}, fileName: {}", institutionId, file.getOriginalFilename());

        MediaFileResponse response = mediaService.uploadMedia(institutionId, userId, file, metadata);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/presigned-upload")
    public ResponseEntity<PresignedUrlResponse> getPresignedUploadUrl(
            @RequestParam("fileName") String fileName,
            @RequestParam("contentType") String contentType,
            @RequestAttribute("institutionId") UUID institutionId) {
        PresignedUrlResponse response = mediaService.getPresignedUploadUrl(institutionId, fileName, contentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaFileResponse> getMedia(
            @PathVariable Long id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        MediaFileResponse response = mediaService.getMedia(id, institutionId, userRole);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable Long id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        PresignedUrlResponse response = mediaService.getPresignedDownloadUrl(id, institutionId, userRole);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMedia(
            @PathVariable Long id,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        mediaService.deleteMedia(id, institutionId, userRole);
        return ResponseEntity.ok(Map.of("message", "Media deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<List<MediaFileResponse>> listMedia(
            @RequestParam(value = "institutionId", required = false) String ignoredInstitutionId,
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestAttribute("institutionId") UUID institutionId) {
        List<MediaFileResponse> responses = mediaService.listMedia(institutionId, contentType);
        return ResponseEntity.ok(responses);
    }
}
