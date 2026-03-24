package me.jianwen.mediask.infra.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.cache.CacheKeyGenerator;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.port.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String VALUE_DELIMITER = ":";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            "local currentValue = redis.call('GET', KEYS[1]) "
                    + "if not currentValue then return 0 end "
                    + "local delimiterIndex = string.find(currentValue, ARGV[1], 1, true) "
                    + "if not delimiterIndex then return 0 end "
                    + "local currentSecret = string.sub(currentValue, 1, delimiterIndex - 1) "
                    + "if currentSecret ~= ARGV[2] then return 0 end "
                    + "redis.call('SET', KEYS[2], ARGV[3], 'PX', ARGV[4]) "
                    + "redis.call('DEL', KEYS[1]) "
                    + "return 1",
            Long.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    public RedisRefreshTokenStore(StringRedisTemplate stringRedisTemplate, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
    }

    @Override
    public void save(RefreshTokenSession refreshTokenSession) {
        Duration ttl = Duration.between(clock.instant(), refreshTokenSession.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                CacheKeyGenerator.refreshToken(refreshTokenSession.userId(), refreshTokenSession.tokenId()),
                serialize(refreshTokenSession),
                ttl);
    }

    @Override
    public boolean rotate(String currentRefreshToken, RefreshTokenSession nextRefreshTokenSession) {
        RefreshTokenSession.ParsedToken parsedCurrentToken = parse(currentRefreshToken);
        if (parsedCurrentToken == null) {
            return false;
        }
        Duration ttl = Duration.between(clock.instant(), nextRefreshTokenSession.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return false;
        }
        Long result = stringRedisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(
                        CacheKeyGenerator.refreshToken(parsedCurrentToken.userId(), parsedCurrentToken.tokenId()),
                        CacheKeyGenerator.refreshToken(nextRefreshTokenSession.userId(), nextRefreshTokenSession.tokenId())),
                VALUE_DELIMITER,
                parsedCurrentToken.tokenSecret(),
                serialize(nextRefreshTokenSession),
                String.valueOf(ttl.toMillis()));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public Optional<RefreshTokenSession> findByTokenValue(String refreshToken) {
        RefreshTokenSession.ParsedToken parsedToken = parse(refreshToken);
        if (parsedToken == null) {
            return Optional.empty();
        }
        String storedValue =
                stringRedisTemplate.opsForValue().get(CacheKeyGenerator.refreshToken(parsedToken.userId(), parsedToken.tokenId()));
        return deserialize(parsedToken, storedValue);
    }

    @Override
    public void deleteByTokenValue(String refreshToken) {
        RefreshTokenSession.ParsedToken parsedToken = parse(refreshToken);
        if (parsedToken == null) {
            return;
        }
        stringRedisTemplate.delete(CacheKeyGenerator.refreshToken(parsedToken.userId(), parsedToken.tokenId()));
    }

    private String serialize(RefreshTokenSession refreshTokenSession) {
        return refreshTokenSession.tokenSecret() + VALUE_DELIMITER + refreshTokenSession.expiresAt().toEpochMilli();
    }

    private Optional<RefreshTokenSession> deserialize(RefreshTokenSession.ParsedToken parsedToken, String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return Optional.empty();
        }
        int separatorIndex = storedValue.lastIndexOf(VALUE_DELIMITER);
        if (separatorIndex <= 0 || separatorIndex >= storedValue.length() - 1) {
            return Optional.empty();
        }
        String tokenSecret = storedValue.substring(0, separatorIndex);
        String expiresAtValue = storedValue.substring(separatorIndex + 1);
        if (!tokenSecret.equals(parsedToken.tokenSecret())) {
            return Optional.empty();
        }
        try {
            Instant expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresAtValue));
            return Optional.of(
                    new RefreshTokenSession(parsedToken.userId(), parsedToken.tokenId(), parsedToken.tokenSecret(), expiresAt));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private RefreshTokenSession.ParsedToken parse(String refreshToken) {
        try {
            return RefreshTokenSession.parseTokenValue(refreshToken);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
