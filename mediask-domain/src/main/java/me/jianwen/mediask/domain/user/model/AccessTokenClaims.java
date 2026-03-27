package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AccessTokenClaims(Long userId, String tokenId, String sessionId, Instant expiresAt) {

    public AccessTokenClaims {
        userId = ArgumentChecks.requirePositive(userId, "userId");
        tokenId = ArgumentChecks.requireNonBlank(tokenId, "tokenId");
        sessionId = ArgumentChecks.blankToNull(sessionId);
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
