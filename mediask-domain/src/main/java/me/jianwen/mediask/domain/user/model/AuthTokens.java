package me.jianwen.mediask.domain.user.model;

import java.util.Objects;

public record AuthTokens(AccessToken accessToken, RefreshTokenSession refreshTokenSession) {

    public AuthTokens {
        accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        refreshTokenSession = Objects.requireNonNull(refreshTokenSession, "refreshTokenSession must not be null");
    }
}
