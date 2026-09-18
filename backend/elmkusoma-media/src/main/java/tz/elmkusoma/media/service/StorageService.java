package tz.elmkusoma.media.service;

import tz.elmkusoma.media.dto.MediaFileInfo;

import java.io.InputStream;

public interface StorageService {

    String uploadFile(InputStream inputStream, String objectName, String contentType, long size);

    InputStream downloadFile(String objectName);

    String getPresignedUploadUrl(String objectName, String contentType, int expirationMinutes);

    String getPresignedDownloadUrl(String objectName, int expirationMinutes);

    void deleteFile(String objectName);

    MediaFileInfo getFileInfo(String objectName);
}
