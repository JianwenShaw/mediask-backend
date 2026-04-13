package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionTurnDetail(
        Long turnId,
        Integer turnNo,
        AiTurnStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer errorCode,
        String errorMessage,
        List<AiSessionMessage> messages) {

    public AiSessionTurnDetail {
        turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        turnNo = ArgumentChecks.normalizePositive(turnNo, "turnNo");
        status = Objects.requireNonNull(status, "status must not be null");
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        messages = messages == null ? List.of() : List.copyOf(messages);
    }
}
