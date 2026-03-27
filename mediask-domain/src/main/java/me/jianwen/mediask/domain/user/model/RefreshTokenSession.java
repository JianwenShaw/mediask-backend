package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record RefreshTokenSession(Long userId, String tokenId, String tokenSecret, Instant expiresAt) {

    private static final String TOKEN_PREFIX = "rt";
    private static final String DELIMITER = ".";

    public RefreshTokenSession {
        userId = ArgumentChecks.requirePositive(userId, "userId");
        tokenId = ArgumentChecks.requireNonBlank(tokenId, "tokenId");
        tokenSecret = ArgumentChecks.requireNonBlank(tokenSecret, "tokenSecret");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public String tokenValue() {
        return String.join(DELIMITER, TOKEN_PREFIX, String.valueOf(userId), tokenId, tokenSecret);
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public static ParsedToken parseTokenValue(String tokenValue) {
        String normalized = ArgumentChecks.requireNonBlank(tokenValue, "refreshToken");
        String[] parts = normalized.split("\\.");
        if (parts.length != 4 || !TOKEN_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("refreshToken format is invalid");
        }
        return new ParsedToken(
                ArgumentChecks.requirePositive(Long.parseLong(parts[1]), "userId"),
                ArgumentChecks.requireNonBlank(parts[2], "tokenId"),
                ArgumentChecks.requireNonBlank(parts[3], "tokenSecret"));
    }

    public record ParsedToken(Long userId, String tokenId, String tokenSecret) {

        public ParsedToken {
            userId = ArgumentChecks.requirePositive(userId, "userId");
            tokenId = ArgumentChecks.requireNonBlank(tokenId, "tokenId");
            tokenSecret = ArgumentChecks.requireNonBlank(tokenSecret, "tokenSecret");
        }
    }
}
