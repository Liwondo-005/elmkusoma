package tz.elmkusoma.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.media.domain.MediaFile;
import tz.elmkusoma.media.dto.MediaFileResponse;
import tz.elmkusoma.media.dto.PresignedUrlResponse;
import tz.elmkusoma.media.repository.MediaFileRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final StorageService storageService;
    private final MediaFileRepository mediaFileRepository;

    @Value("${minio.bucket:elmkusoma-media}")
    private String bucket;

    @Transactional
    public MediaFileResponse uploadMedia(Long institutionId, Long userId, MultipartFile file, String metadata) {
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String objectKey = buildObjectKey(institutionId, fileName);

            String url = storageService.uploadFile(
                    file.getInputStream(),
                    objectKey,
                    file.getContentType(),
                    file.getSize()
            );

            MediaFile mediaFile = MediaFile.builder()
                    .institutionId(institutionId)
                    .userId(userId)
                    .fileName(fileName)
                    .originalName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .objectKey(objectKey)
                    .bucket(bucket)
                    .url(url)
                    .metadata(metadata)
                    .isDeleted(false)
                    .build();

            mediaFile = mediaFileRepository.save(mediaFile);

            log.info("Media uploaded successfully: id={}, institutionId={}", mediaFile.getId(), institutionId);

            return mapToResponse(mediaFile);
        } catch (IOException e) {
            log.error("Error reading file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload media", e);
        }
    }

    @Transactional(readOnly = true)
    public MediaFileResponse getMedia(Long mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        MediaFileResponse response = mapToResponse(mediaFile);

        String presignedUrl = storageService.getPresignedDownloadUrl(mediaFile.getObjectKey(), 60);
        response.setUrl(presignedUrl);

        return response;
    }

    @Transactional(readOnly = true)
    public List<MediaFileResponse> listMedia(Long institutionId, String contentType) {
        List<MediaFile> mediaFiles;
        if (contentType != null && !contentType.isEmpty()) {
            mediaFiles = mediaFileRepository.findByInstitutionIdAndContentTypeAndIsDeletedFalseOrderByCreatedAtDesc(
                    institutionId, contentType);
        } else {
            mediaFiles = mediaFileRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        }

        return mediaFiles.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        mediaFile.setIsDeleted(true);
        mediaFileRepository.save(mediaFile);

        try {
            storageService.deleteFile(mediaFile.getObjectKey());
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO: {}", e.getMessage());
        }

        log.info("Media soft-deleted: id={}", mediaId);
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse getPresignedUploadUrl(Long institutionId, String fileName, String contentType) {
        String objectKey = buildObjectKey(institutionId, fileName);
        String uploadUrl = storageService.getPresignedUploadUrl(objectKey, contentType, 15);

        return PresignedUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Transactional(readOnly = true)
    public PresignedUrlResponse getPresignedDownloadUrl(Long mediaId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaId)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));

        String downloadUrl = storageService.getPresignedDownloadUrl(mediaFile.getObjectKey(), 60);

        return PresignedUrlResponse.builder()
                .downloadUrl(downloadUrl)
                .objectKey(mediaFile.getObjectKey())
                .expiresAt(LocalDateTime.now().plusMinutes(60))
                .build();
    }

    private String generateFileName(String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    private String buildObjectKey(Long institutionId, String fileName) {
        return String.format("%d/media/%s", institutionId, fileName);
    }

    private MediaFileResponse mapToResponse(MediaFile mediaFile) {
        return MediaFileResponse.builder()
                .id(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .originalName(mediaFile.getOriginalName())
                .contentType(mediaFile.getContentType())
                .fileSize(mediaFile.getFileSize())
                .url(mediaFile.getUrl())
                .thumbnailUrl(mediaFile.getThumbnailUrl())
                .uploadedAt(mediaFile.getCreatedAt())
                .build();
    }
}
