package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;

public record AccessToken(String value, String tokenId, Instant expiresAt) {

    public AccessToken {
        value = requireNonBlank(value, "value");
        tokenId = requireNonBlank(tokenId, "tokenId");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
