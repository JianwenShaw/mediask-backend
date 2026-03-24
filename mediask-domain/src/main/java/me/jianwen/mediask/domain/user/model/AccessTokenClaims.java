package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;

public record AccessTokenClaims(Long userId, String tokenId, Instant expiresAt) {

    public AccessTokenClaims {
        userId = requirePositive(userId, "userId");
        tokenId = requireNonBlank(tokenId, "tokenId");
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
}
