package tz.elmkusoma.media.service.impl;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.elmkusoma.media.dto.MediaFileInfo;
import tz.elmkusoma.media.service.StorageService;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:elmkusoma-media}")
    private String bucket;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Override
    public String uploadFile(InputStream inputStream, String objectName, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            String url = String.format("%s/%s/%s", endpoint, bucket, objectName);
            log.info("Uploaded file: {} to bucket: {}", objectName, bucket);
            return url;
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }

    @Override
    public String getPresignedUploadUrl(String objectName, String contentType, int expirationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned upload URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    @Override
    public String getPresignedDownloadUrl(String objectName, int expirationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned download URL: {}", e.getMessage());
            throw new RuntimeException("Failed to generate presigned download URL", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted file: {} from bucket: {}", objectName, bucket);
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    @Override
    public MediaFileInfo getFileInfo(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );

            return MediaFileInfo.builder()
                    .objectName(objectName)
                    .contentType(stat.contentType())
                    .size(stat.size())
                    .lastModified(Instant.ofEpochMilli(stat.lastModified().toInstant().toEpochMilli()))
                    .build();
        } catch (Exception e) {
            log.error("Error getting file info: {}", e.getMessage());
            throw new RuntimeException("Failed to get file info from MinIO", e);
        }
    }
}
