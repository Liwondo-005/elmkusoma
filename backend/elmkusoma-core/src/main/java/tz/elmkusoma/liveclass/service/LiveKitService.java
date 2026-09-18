package tz.elmkusoma.liveclass.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class LiveKitService {

    private final LiveKitConfig config;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LiveKitService(LiveKitConfig config) {
        this.config = config;
    }

    public boolean isAvailable() {
        return config.isConfigured();
    }

    public String generateToken(UUID classId, UUID userId, String identity, boolean isTeacher) {
        if (!config.isConfigured()) {
            log.warn("LiveKit not configured, cannot generate token");
            return null;
        }

        try {
            String roomName = "liveclass-" + classId;
            String apiKey = config.getServer().getApiKey();
            String apiSecret = config.getServer().getApiSecret();

            SecretKeySpec keySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8),
                    SignatureAlgorithm.HS256.getJcaName()
            );

            Date now = new Date();
            Date expiry = new Date(now.getTime() + 2 * 60 * 60 * 1000);

            Map<String, Object> videoGrants = new HashMap<>();
            videoGrants.put("roomJoin", true);
            videoGrants.put("room", roomName);

            if (isTeacher) {
                videoGrants.put("roomCreate", true);
                videoGrants.put("roomAdmin", true);
                videoGrants.put("canPublish", true);
                videoGrants.put("canPublishData", true);
                videoGrants.put("canSubscribe", true);
                videoGrants.put("canUpdateOwnMetadata", true);
            } else {
                videoGrants.put("canPublish", false);
                videoGrants.put("canPublishData", true);
                videoGrants.put("canSubscribe", true);
                videoGrants.put("canUpdateOwnMetadata", true);
            }

            Map<String, Object> claims = new HashMap<>();
            claims.put("video", videoGrants);

            String jwt = Jwts.builder()
                    .setHeaderParam("alg", "HS256")
                    .setHeaderParam("typ", "JWT")
                    .setIssuer(apiKey)
                    .setSubject(identity)
                    .setIssuedAt(now)
                    .setNotBefore(now)
                    .setExpiration(expiry)
                    .addClaims(claims)
                    .signWith(keySpec, SignatureAlgorithm.HS256)
                    .compact();

            log.info("Generated LiveKit token for class={}, user={}, teacher={}", classId, userId, isTeacher);
            return jwt;

        } catch (Exception e) {
            log.error("Failed to generate LiveKit token for class={} user={}", classId, userId, e);
            return null;
        }
    }

    public String generateRoomName(UUID classId) {
        return "liveclass-" + classId;
    }

    public String getServerUrl() {
        return config.getServer().getUrl();
    }

    public String startRecording(UUID classId) {
        if (!config.isEgressEnabled()) {
            log.warn("Egress not enabled, cannot start recording");
            return null;
        }
        if (!config.isConfigured()) {
            log.warn("LiveKit not configured, cannot start recording");
            return null;
        }

        try {
            String roomName = generateRoomName(classId);
            String outputPath = config.getEgress().getOutputPath() + classId + "/%c";
            String bucket = config.getEgress().getOutputBucket();

            Map<String, Object> fileOutput = new HashMap<>();
            if (bucket != null && !bucket.isEmpty()) {
                Map<String, Object> s3 = new HashMap<>();
                s3.put("bucket", bucket);
                s3.put("key", config.getEgress().getOutputPath() + classId);
                fileOutput.put("s3", s3);
            } else {
                fileOutput.put("filepath", outputPath);
            }

            Map<String, Object> request = new HashMap<>();
            request.put("roomName", roomName);
            request.put("layout", "speaker");
            request.put("fileOutputs", List.of(fileOutput));

            String body = objectMapper.writeValueAsString(request);
            String result = callTwirp("StartRoomCompositeEgress", body);

            if (result != null) {
                JsonNode json = objectMapper.readTree(result);
                String egressId = json.path("egressId").asText(null);
                log.info("Started recording for class={}, egressId={}", classId, egressId);
                return egressId;
            }
            return null;

        } catch (Exception e) {
            log.error("Failed to start recording for class={}", classId, e);
            return null;
        }
    }

    public boolean stopRecording(String egressId) {
        if (!config.isEgressEnabled() || egressId == null || egressId.isBlank()) {
            return false;
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("egressId", egressId);

            String body = objectMapper.writeValueAsString(request);
            String result = callTwirp("StopEgress", body);

            if (result != null) {
                log.info("Stopped recording egressId={}", egressId);
                return true;
            }
            return false;

        } catch (Exception e) {
            log.error("Failed to stop recording egressId={}", egressId, e);
            return false;
        }
    }

    private String callTwirp(String method, String jsonBody) {
        try {
            String url = config.getServer().getUrl()
                    .replace("ws://", "http://")
                    .replace("wss://", "https://");

            String authHeader = generateServerAuthHeader();
            if (authHeader == null) {
                log.error("Cannot authenticate with LiveKit server");
                return null;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/twirp/livekit.Egress/" + method))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                log.error("LiveKit Egress API error: status={}, body={}", response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("LiveKit Egress API call failed: {}", method, e);
            return null;
        }
    }

    private String generateServerAuthHeader() {
        try {
            String apiKey = config.getServer().getApiKey();
            String apiSecret = config.getServer().getApiSecret();

            SecretKeySpec keySpec = new SecretKeySpec(
                    apiSecret.getBytes(StandardCharsets.UTF_8),
                    SignatureAlgorithm.HS256.getJcaName()
            );

            Date now = new Date();
            Date expiry = new Date(now.getTime() + 60 * 1000);

            Map<String, Object> videoGrants = new HashMap<>();
            videoGrants.put("egress", true);
            videoGrants.put("roomCreate", true);
            videoGrants.put("roomAdmin", true);
            videoGrants.put("roomList", true);

            Map<String, Object> claims = new HashMap<>();
            claims.put("video", videoGrants);

            String jwt = Jwts.builder()
                    .setHeaderParam("alg", "HS256")
                    .setHeaderParam("typ", "JWT")
                    .setIssuer(apiKey)
                    .setSubject("server")
                    .setIssuedAt(now)
                    .setNotBefore(now)
                    .setExpiration(expiry)
                    .addClaims(claims)
                    .signWith(keySpec, SignatureAlgorithm.HS256)
                    .compact();

            return "Bearer " + jwt;

        } catch (Exception e) {
            log.error("Failed to generate server auth header", e);
            return null;
        }
    }
}
