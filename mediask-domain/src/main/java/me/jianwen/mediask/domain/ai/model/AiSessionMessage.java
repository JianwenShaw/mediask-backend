package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;

public record AiSessionMessage(
        String role,
        String content,
        OffsetDateTime createdAt) {}
