package me.jianwen.mediask.domain.user.model;

import java.time.Instant;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AccessToken(String value, String tokenId, Instant expiresAt) {

    public AccessToken {
        value = ArgumentChecks.requireNonBlank(value, "value");
        tokenId = ArgumentChecks.requireNonBlank(tokenId, "tokenId");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
