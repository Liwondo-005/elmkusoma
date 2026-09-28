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

    /**
     * Uploads on behalf of the caller: the media service binds institution/user
     * from the forwarded JWT, never from form fields.
     */
    public Map<String, Object> uploadFile(MultipartFile file, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.set("Authorization", bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken);
            }
            
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", file.getResource());
            
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

    public Map<String, Object> getPresignedUploadUrl(String fileName, String contentType, String bearerToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.set("Authorization", bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken);
            }
            
            Map<String, String> request = Map.of(
                "fileName", fileName,
                "contentType", contentType
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

    public Map<String, Object> getDownloadUrl(String mediaId, String bearerToken) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (bearerToken != null && !bearerToken.isBlank()) {
                headers.set("Authorization", bearerToken.startsWith("Bearer ") ? bearerToken : "Bearer " + bearerToken);
            }
            ResponseEntity<Map> response = restTemplate.exchange(
                mediaServiceUrl + "/api/v1/media/" + mediaId + "/download",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to get download URL from media service: {}", e.getMessage());
            throw new RuntimeException("Media service unavailable", e);
        }
    }
}
