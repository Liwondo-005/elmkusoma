package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorePresenceService {

    @Autowired
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_PREFIX = "online:";
    private static final String PRESENCE_PREFIX = "presence:";

    public void userOnline(UUID userId, UUID institutionId) {
        try {
            String onlineKey = ONLINE_PREFIX + institutionId;
            String presenceKey = PRESENCE_PREFIX + userId;

            redisTemplate.opsForSet().add(onlineKey, userId.toString());

            Map<String, Object> presenceData = new HashMap<>();
            presenceData.put("userId", userId.toString());
            presenceData.put("institutionId", institutionId.toString());
            presenceData.put("connectedAt", LocalDateTime.now().toString());

            redisTemplate.opsForHash().putAll(presenceKey, presenceData);

            log.debug("User {} marked online for institution {}", userId, institutionId);
        } catch (Exception e) {
            // Redis is optional (documented in .env): degrade gracefully, never break the caller
            log.warn("Presence online update skipped for user {}: {}", userId, e.getMessage());
        }
    }

    public void userOffline(UUID userId) {
        try {
            String presenceKey = PRESENCE_PREFIX + userId;

            Object institutionId = redisTemplate.opsForHash().get(presenceKey, "institutionId");
            if (institutionId != null) {
                String onlineKey = ONLINE_PREFIX + institutionId;
                redisTemplate.opsForSet().remove(onlineKey, userId.toString());
            }

            redisTemplate.delete(presenceKey);

            log.debug("User {} marked offline", userId);
        } catch (Exception e) {
            log.warn("Presence offline update skipped for user {}: {}", userId, e.getMessage());
        }
    }

    public Set<Object> getOnlineUsers(UUID institutionId) {
        try {
            String onlineKey = ONLINE_PREFIX + institutionId;
            return redisTemplate.opsForSet().members(onlineKey);
        } catch (Exception e) {
            log.warn("Presence query skipped for institution {}: {}", institutionId, e.getMessage());
            return Collections.emptySet();
        }
    }

    public boolean isOnline(UUID userId) {
        try {
            String presenceKey = PRESENCE_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey));
        } catch (Exception e) {
            log.warn("Presence check skipped for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
