package tz.elmkusoma.liveclass.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class LiveKitService {

    private final LiveKitConfig config;

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
}
