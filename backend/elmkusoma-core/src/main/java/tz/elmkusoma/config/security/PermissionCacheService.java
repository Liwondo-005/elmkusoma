package tz.elmkusoma.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.config.TenantAwareRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final TenantAwareRedisTemplate redisTemplate;

    private static final String PERMISSION_CACHE_PREFIX = "perm:";
    private static final String ROLE_CACHE_PREFIX = "role:";
    private static final String MEMBERSHIP_CACHE_PREFIX = "membership:";
    private static final long CACHE_TTL_MINUTES = 15;

    public void cacheUserPermissions(UUID userId, UUID institutionId, Object permissions) {
        String key = PERMISSION_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.set(key, permissions, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Object getCachedPermissions(UUID userId, UUID institutionId) {
        String key = PERMISSION_CACHE_PREFIX + userId + ":" + institutionId;
        return redisTemplate.get(key);
    }

    public void invalidateUserPermissions(UUID userId, UUID institutionId) {
        String key = PERMISSION_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.delete(key);
        log.debug("Invalidated permissions cache for user {} in institution {}", userId, institutionId);
    }

    public void invalidateAllUserPermissions(UUID userId) {
        String pattern = PERMISSION_CACHE_PREFIX + userId + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
        log.debug("Invalidated all permissions caches for user {}", userId);
    }

    public void cacheUserRole(UUID userId, UUID institutionId, String role) {
        String key = ROLE_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.set(key, role, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public String getCachedRole(UUID userId, UUID institutionId) {
        String key = ROLE_CACHE_PREFIX + userId + ":" + institutionId;
        return (String) redisTemplate.get(key);
    }

    public void invalidateUserRole(UUID userId, UUID institutionId) {
        String key = ROLE_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.delete(key);
    }

    public void invalidateAllUserRoles(UUID userId) {
        String pattern = ROLE_CACHE_PREFIX + userId + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }

    public void cacheMembership(UUID userId, UUID institutionId, Object membership) {
        String key = MEMBERSHIP_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.set(key, membership, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public Object getCachedMembership(UUID userId, UUID institutionId) {
        String key = MEMBERSHIP_CACHE_PREFIX + userId + ":" + institutionId;
        return redisTemplate.get(key);
    }

    public void invalidateMembership(UUID userId, UUID institutionId) {
        String key = MEMBERSHIP_CACHE_PREFIX + userId + ":" + institutionId;
        redisTemplate.delete(key);
    }

    public void invalidateAllMemberships(UUID userId) {
        String pattern = MEMBERSHIP_CACHE_PREFIX + userId + ":*";
        redisTemplate.keys(pattern).forEach(redisTemplate::delete);
    }

    public void invalidateAllForInstitution(UUID institutionId) {
        String permPattern = PERMISSION_CACHE_PREFIX + "*:" + institutionId;
        String rolePattern = ROLE_CACHE_PREFIX + "*:" + institutionId;
        String membershipPattern = MEMBERSHIP_CACHE_PREFIX + "*:" + institutionId;

        redisTemplate.keys(permPattern).forEach(redisTemplate::delete);
        redisTemplate.keys(rolePattern).forEach(redisTemplate::delete);
        redisTemplate.keys(membershipPattern).forEach(redisTemplate::delete);

        log.info("Invalidated all caches for institution {}", institutionId);
    }
}