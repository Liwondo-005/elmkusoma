package tz.elmkusoma.liveclass.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import javax.crypto.Mac;
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

    private static final long PARTICIPANT_TOKEN_TTL_MS = 15 * 60 * 1000;

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
        return generateTokenForRoom(generateRoomName(classId), userId, identity, isTeacher);
    }

    public String generateEventToken(UUID eventId, UUID userId, String identity, boolean isTeacher) {
        return generateTokenForRoom(generateRoomNameForEvent(eventId), userId, identity, isTeacher);
    }

    private String generateTokenForRoom(String roomName, UUID userId, String identity, boolean isTeacher) {
        if (!config.isConfigured()) {
            log.warn("LiveKit not configured, cannot generate token");
            return null;
        }

        try {
            String apiKey = config.getServer().getApiKey();
            String apiSecret = config.getServer().getApiSecret();

            Date now = new Date();
            Date expiry = new Date(now.getTime() + PARTICIPANT_TOKEN_TTL_MS);

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
            claims.put("iss", apiKey);
            claims.put("sub", identity);
            claims.put("iat", now.getTime() / 1000);
            claims.put("nbf", now.getTime() / 1000);
            claims.put("exp", expiry.getTime() / 1000);
            claims.put("video", videoGrants);

            String jwt = signHs256(claims, apiSecret);

            log.info("Generated LiveKit token for room={}, user={}, teacher={}", roomName, userId, isTeacher);
            return jwt;

        } catch (Exception e) {
            log.error("Failed to generate LiveKit token for room={} user={}", roomName, userId, e);
            return null;
        }
    }

    public String generateRoomName(UUID classId) {
        return "liveclass-" + classId;
    }

    public String generateRoomNameForEvent(UUID eventId) {
        return "event-" + eventId;
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

            Date now = new Date();
            Date expiry = new Date(now.getTime() + 60 * 1000);

            Map<String, Object> videoGrants = new HashMap<>();
            videoGrants.put("egress", true);
            videoGrants.put("roomCreate", true);
            videoGrants.put("roomAdmin", true);
            videoGrants.put("roomList", true);

            Map<String, Object> claims = new HashMap<>();
            claims.put("iss", apiKey);
            claims.put("sub", "server");
            claims.put("iat", now.getTime() / 1000);
            claims.put("nbf", now.getTime() / 1000);
            claims.put("exp", expiry.getTime() / 1000);
            claims.put("video", videoGrants);

            String jwt = signHs256(claims, apiSecret);
            return "Bearer " + jwt;

        } catch (Exception e) {
            log.error("Failed to generate server auth header", e);
            return null;
        }
    }

    /**
     * Signs a JWT with HS256 using raw JCA Mac. Unlike jjwt, this accepts LiveKit's
     * short API secrets (e.g. devsecret) — LiveKit server itself signs/verifies with
     * the raw secret bytes with no minimum key size.
     */
    private String signHs256(Map<String, Object> claims, String apiSecret) throws Exception {
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        String headerJson = objectMapper.writeValueAsString(header);
        String payloadJson = objectMapper.writeValueAsString(claims);
        String signingInput = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8))
                + "." + base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));

        return signingInput + "." + base64UrlEncode(signature);
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
