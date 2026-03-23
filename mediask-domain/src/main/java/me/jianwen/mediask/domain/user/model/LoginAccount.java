package me.jianwen.mediask.domain.user.model;

import java.util.Objects;

public record LoginAccount(AuthenticatedUser authenticatedUser, String passwordHash, AccountStatus accountStatus) {

    public LoginAccount {
        authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
        passwordHash = requireNonBlank(passwordHash, "passwordHash");
        accountStatus = Objects.requireNonNull(accountStatus, "accountStatus must not be null");
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
