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
                // Learners are active participants: camera + microphone publishing is required
                // for the interactive classroom (raise-hand-to-speak, teacher sees/hears learner).
                // roomAdmin/roomCreate stay teacher-only.
                videoGrants.put("canPublish", true);
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
            // Spec: "Recording: Enabled only if configured." LiveKit requires every file
            // output to carry a storage destination oneof (else the API rejects with
            // "invalid_argument: request has missing or invalid field: output") and it uses
            // exactly the credentials we send - so starting without them would only create
            // an egress that dies at upload with S3 403 InvalidAccessKeyId.
            if (!config.getEgress().hasStorage()) {
                log.warn("Recording storage not configured - set LIVEKIT_EGRESS_BUCKET and "
                        + "LIVEKIT_EGRESS_S3_ACCESS_KEY/SECRET/REGION (and optionally ENDPOINT). "
                        + "Skipping egress start for class={}", classId);
                return null;
            }

            // Egress needs the room to exist; CreateRoom is idempotent (roomCreate grant).
            ensureRoom(classId);
            String roomName = generateRoomName(classId);
            String outputPath = config.getEgress().getOutputPath() + classId + "/%c";

            Map<String, Object> s3 = new HashMap<>();
            s3.put("accessKey", config.getEgress().getAccessKey());
            s3.put("secret", config.getEgress().getSecret());
            s3.put("bucket", config.getEgress().getOutputBucket());
            s3.put("region", config.getEgress().getRegion());
            if (config.getEgress().getEndpoint() != null && !config.getEgress().getEndpoint().isEmpty()) {
                s3.put("endpoint", config.getEgress().getEndpoint());
                s3.put("forcePathStyle", true);
            }

            // EncodedFileOutput requires the storage `output` oneof (s3/gcp/azure/aliOSS);
            // filepath alone is only the key inside the bucket.
            Map<String, Object> fileOutput = new HashMap<>();
            fileOutput.put("filepath", outputPath);
            fileOutput.put("s3", s3);

            Map<String, Object> request = new HashMap<>();
            request.put("roomName", roomName);
            request.put("layout", "speaker");
            request.put("fileOutputs", List.of(fileOutput));

            String body = objectMapper.writeValueAsString(request);
            String result = callTwirp("livekit.Egress", "StartRoomCompositeEgress", body);

            if (result != null) {
                JsonNode json = objectMapper.readTree(result);
                // Twirp responses use proto json names (snake_case).
                String egressId = json.path("egress_id").asText(json.path("egressId").asText(null));
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
            String result = callTwirp("livekit.Egress", "StopEgress", body);

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

    /**
     * Ensures the LiveKit room exists so Egress can attach to it (CreateRoom is idempotent).
     */
    public void ensureRoom(UUID classId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("name", generateRoomName(classId));
            String body = objectMapper.writeValueAsString(request);
            String result = callTwirp("livekit.RoomService", "CreateRoom", body);
            log.info("Ensured LiveKit room for class {}: {}", classId, result != null ? "created/exists" : "failed");
        } catch (Exception e) {
            log.warn("Ensure room failed for class {}: {}", classId, e.getMessage());
        }
    }

    /**
     * True when recording can actually run: egress enabled AND a storage destination
     * (bucket + credentials) is configured. LiveKit rejects file outputs without one.
     */
    public boolean isRecordingConfigured() {
        return config.isEgressEnabled() && config.getEgress().hasStorage();
    }

    /**
     * Polls ListEgress until the egress completes and returns the final file URL,
     * or null if it fails / does not become available in time.
     * NOTE: there is no GetEgress RPC - LiveKit answers it with 404 bad_route, so the
     * status must be read from ListEgress filtered by egress_id (snake_case response).
     */
    public String resolveRecordingUrl(String egressId) {
        if (!config.isEgressEnabled() || egressId == null || egressId.isBlank()) {
            return null;
        }
        for (int attempt = 0; attempt < 15; attempt++) {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("egress_id", egressId);
                String result = callTwirp("livekit.Egress", "ListEgress", objectMapper.writeValueAsString(request));
                if (result != null) {
                    JsonNode json = objectMapper.readTree(result);
                    JsonNode info = null;
                    JsonNode items = json.path("items");
                    if (items.isArray()) {
                        for (JsonNode it : items) {
                            if (egressId.equals(it.path("egress_id").asText())) {
                                info = it;
                                break;
                            }
                        }
                        if (info == null && items.size() == 1) {
                            info = items.get(0);
                        }
                    }
                    if (info != null) {
                        JsonNode files = info.path("file_results");
                        if (!files.isArray() || files.size() == 0) {
                            files = info.path("fileResults");
                        }
                        if (files.isArray()) {
                            for (JsonNode f : files) {
                                String url = f.path("location").asText(null);
                                if (url == null) url = f.path("url").asText(null);
                                if (url != null && url.startsWith("http")) {
                                    log.info("Resolved recording URL for egress {} (attempt {})", egressId, attempt);
                                    return url;
                                }
                            }
                        }
                        String status = info.path("status").asText("");
                        String error = info.path("error").asText("");
                        if ("EGRESS_FAILED".equalsIgnoreCase(status)
                                || "EGRESS_ABORTED".equalsIgnoreCase(status)
                                || "FAILED".equalsIgnoreCase(status)) {
                            log.warn("Egress {} failed: {}", egressId, error);
                            return null;
                        }
                        if (attempt == 0) {
                            log.info("Egress {} status={} (waiting for file result)", egressId, status);
                        }
                    }
                }
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                log.warn("ListEgress attempt {} for {} failed: {}", attempt, egressId, e.getMessage());
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        log.warn("Recording URL not available for egress {} within timeout", egressId);
        return null;
    }

    private String callTwirp(String method, String jsonBody) {
        return callTwirp("livekit.Egress", method, jsonBody);
    }

    private String callTwirp(String service, String method, String jsonBody) {
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
                    .uri(URI.create(url + "/twirp/" + service + "/" + method))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                log.error("LiveKit {} API error: status={}, body={}", method, response.statusCode(), response.body());
                return null;
            }

        } catch (Exception e) {
            log.error("LiveKit API call failed: {}", method, e);
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
            // LiveKit checks video.roomRecord (EnsureRecordPermission) for Egress API calls.
            // There is no "egress" grant field - unknown fields are silently ignored.
            videoGrants.put("roomRecord", true);
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
