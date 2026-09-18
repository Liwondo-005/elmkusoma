package tz.elmkusoma.realtime.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceHandler {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_PREFIX = "online:";
    private static final String PRESENCE_PREFIX = "presence:";

    public void userConnected(String userId, String institutionId) {
        String onlineKey = ONLINE_PREFIX + institutionId;
        String presenceKey = PRESENCE_PREFIX + userId;

        redisTemplate.opsForSet().add(onlineKey, userId);
        redisTemplate.opsForHash().put(presenceKey, "userId", userId);
        redisTemplate.opsForHash().put(presenceKey, "institutionId", institutionId);
        redisTemplate.opsForHash().put(presenceKey, "status", "ONLINE");

        log.info("User {} connected to institution {}", userId, institutionId);
    }

    public void userDisconnected(String userId) {
        String presenceKey = PRESENCE_PREFIX + userId;
        Map<Object, Object> presenceData = redisTemplate.opsForHash().entries(presenceKey);

        if (!presenceData.isEmpty()) {
            String institutionId = (String) presenceData.get("institutionId");
            if (institutionId != null) {
                String onlineKey = ONLINE_PREFIX + institutionId;
                redisTemplate.opsForSet().remove(onlineKey, userId);
            }
        }

        redisTemplate.delete(presenceKey);
        log.info("User {} disconnected", userId);
    }

    @SuppressWarnings("unchecked")
    public Set<Object> getOnlineUsers(String institutionId) {
        String onlineKey = ONLINE_PREFIX + institutionId;
        return redisTemplate.opsForSet().members(onlineKey);
    }

    public boolean isUserOnline(String userId) {
        String presenceKey = PRESENCE_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey));
    }
}
