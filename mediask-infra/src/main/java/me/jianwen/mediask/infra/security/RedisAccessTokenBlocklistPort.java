package me.jianwen.mediask.infra.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.domain.user.port.AccessTokenBlocklistPort;
import org.springframework.data.redis.core.StringRedisTemplate;

public final class RedisAccessTokenBlocklistPort implements AccessTokenBlocklistPort {

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    public RedisAccessTokenBlocklistPort(StringRedisTemplate stringRedisTemplate, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
    }

    @Override
    public void block(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(CacheKeyGenerator.jwtBlacklist(tokenId.trim()), "1", ttl);
    }

    @Override
    public boolean isBlocked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        Boolean exists = stringRedisTemplate.hasKey(CacheKeyGenerator.jwtBlacklist(tokenId.trim()));
        return Boolean.TRUE.equals(exists);
    }
}
