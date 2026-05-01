package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionTurn(
        String turnId,
        Integer turnNo,
        String turnStatus,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String errorCode,
        String errorMessage,
        List<AiSessionMessage> messages) {}
