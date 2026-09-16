package tz.elmkusoma.liveclass.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MediaProxyService {

    @Value("${services.media.url:http://localhost:8083}")
    private String mediaServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> uploadFile(MultipartFile file, UUID institutionId, UUID userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            body.add("institutionId", institutionId.toString());
            body.add("userId", userId.toString());
            
            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> request = 
                new HttpEntity<>(body, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                mediaServiceUrl + "/api/v1/media/upload",
                HttpMethod.POST,
                request,
                Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to proxy upload to media service: {}", e.getMessage());
            throw new RuntimeException("Media service unavailable", e);
        }
    }

    public Map<String, Object> getPresignedUploadUrl(String fileName, String contentType, UUID institutionId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> request = Map.of(
                "fileName", fileName,
                "contentType", contentType,
                "institutionId", institutionId.toString()
            );
            
            HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                mediaServiceUrl + "/api/v1/media/presigned-upload",
                HttpMethod.POST,
                httpEntity,
                Map.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get presigned URL from media service: {}", e.getMessage());
            throw new RuntimeException("Media service unavailable", e);
        }
    }

    public Map<String, Object> getMediaInfo(String mediaId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                mediaServiceUrl + "/api/v1/media/" + mediaId,
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get media info from media service: {}", e.getMessage());
            throw new RuntimeException("Media service unavailable", e);
        }
    }

    public Map<String, Object> getDownloadUrl(String mediaId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                mediaServiceUrl + "/api/v1/media/" + mediaId + "/download",
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get download URL from media service: {}", e.getMessage());
            throw new RuntimeException("Media service unavailable", e);
        }
    }
}
