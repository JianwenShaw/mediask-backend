package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionMessage(AiContentRole role, String encryptedContent, OffsetDateTime createdAt) {

    public AiSessionMessage {
        role = Objects.requireNonNull(role, "role must not be null");
        encryptedContent = ArgumentChecks.requireNonBlank(encryptedContent, "encryptedContent");
        createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
