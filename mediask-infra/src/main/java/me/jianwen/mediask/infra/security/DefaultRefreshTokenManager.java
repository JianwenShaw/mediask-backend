package me.jianwen.mediask.infra.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import me.jianwen.mediask.domain.user.model.RefreshTokenSession;
import me.jianwen.mediask.domain.user.port.RefreshTokenManager;

public final class DefaultRefreshTokenManager implements RefreshTokenManager {

    private final JwtProperties jwtProperties;
    private final Clock clock;

    public DefaultRefreshTokenManager(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Override
    public RefreshTokenSession issue(Long userId) {
        Instant expiresAt = clock.instant().plusSeconds(jwtProperties.refreshTokenExpireDays() * 24L * 60L * 60L);
        return new RefreshTokenSession(userId, randomTokenPart(), randomTokenPart(), expiresAt);
    }

    private String randomTokenPart() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
