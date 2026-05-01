package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionDetail(
        String sessionId,
        String sceneType,
        String status,
        Long departmentId,
        String chiefComplaintSummary,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<AiSessionTurn> turns) {}
