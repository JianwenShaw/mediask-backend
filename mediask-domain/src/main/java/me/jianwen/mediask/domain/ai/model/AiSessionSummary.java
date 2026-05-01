package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;

public record AiSessionSummary(
        String sessionId,
        String sceneType,
        String status,
        Long departmentId,
        String chiefComplaintSummary,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {}
