package me.jianwen.mediask.application.user.usecase;

import java.util.Objects;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;

public record LoginResult(String accessToken, AuthenticatedUser authenticatedUser) {

    public LoginResult {
        accessToken = requireNonBlank(accessToken, "accessToken");
        authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
