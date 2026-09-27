package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tz.elmkusoma.config.security.OrganizationContext;
import tz.elmkusoma.config.security.OrganizationContextHolder;

@Component
@RequiredArgsConstructor
public class TenantAwareRedisTemplate {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrganizationContextHolder contextHolder;

    private String prefixKey(String key) {
        OrganizationContext context = contextHolder.getContext();
        if (context != null && context.getInstitutionId() != null) {
            return "org:" + context.getInstitutionId() + ":" + key;
        }
        return key;
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(prefixKey(key), value);
    }

    public void set(String key, Object value, long timeout, java.util.concurrent.TimeUnit unit) {
        redisTemplate.opsForValue().set(prefixKey(key), value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(prefixKey(key));
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(prefixKey(key));
    }

    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(prefixKey(key));
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(prefixKey(key), delta);
    }

    public java.util.Set<String> keys(String pattern) {
        String prefixedPattern = prefixKey(pattern);
        return redisTemplate.keys(prefixedPattern);
    }

    public RedisTemplate<String, Object> getRawTemplate() {
        return redisTemplate;
    }
}