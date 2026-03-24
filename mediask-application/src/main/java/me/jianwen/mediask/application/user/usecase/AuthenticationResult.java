package me.jianwen.mediask.application.user.usecase;

import java.util.Objects;
import me.jianwen.mediask.domain.user.model.AuthTokens;
import me.jianwen.mediask.domain.user.model.AuthenticatedUser;

public record AuthenticationResult(AuthTokens tokens, AuthenticatedUser authenticatedUser) {

    public AuthenticationResult {
        tokens = Objects.requireNonNull(tokens, "tokens must not be null");
        authenticatedUser = Objects.requireNonNull(authenticatedUser, "authenticatedUser must not be null");
    }
}
