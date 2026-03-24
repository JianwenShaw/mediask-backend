package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;

public record RefreshTokenSession(Long userId, String tokenId, String tokenSecret, Instant expiresAt) {

    private static final String TOKEN_PREFIX = "rt";
    private static final String DELIMITER = ".";

    public RefreshTokenSession {
        userId = requirePositive(userId, "userId");
        tokenId = requireNonBlank(tokenId, "tokenId");
        tokenSecret = requireNonBlank(tokenSecret, "tokenSecret");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    public String tokenValue() {
        return String.join(DELIMITER, TOKEN_PREFIX, String.valueOf(userId), tokenId, tokenSecret);
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public static ParsedToken parseTokenValue(String tokenValue) {
        String normalized = requireNonBlank(tokenValue, "refreshToken");
        String[] parts = normalized.split("\\.");
        if (parts.length != 4 || !TOKEN_PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("refreshToken format is invalid");
        }
        return new ParsedToken(
                requirePositive(Long.parseLong(parts[1]), "userId"),
                requireNonBlank(parts[2], "tokenId"),
                requireNonBlank(parts[3], "tokenSecret"));
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public record ParsedToken(Long userId, String tokenId, String tokenSecret) {

        public ParsedToken {
            userId = requirePositive(userId, "userId");
            tokenId = requireNonBlank(tokenId, "tokenId");
            tokenSecret = requireNonBlank(tokenSecret, "tokenSecret");
        }
    }
}
