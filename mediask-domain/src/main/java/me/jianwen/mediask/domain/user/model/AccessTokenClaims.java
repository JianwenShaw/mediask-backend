package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;

public record AccessTokenClaims(Long userId, String tokenId, String sessionId, Instant expiresAt) {

    public AccessTokenClaims {
        userId = requirePositive(userId, "userId");
        tokenId = requireNonBlank(tokenId, "tokenId");
        sessionId = normalizeOptional(sessionId);
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
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

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
