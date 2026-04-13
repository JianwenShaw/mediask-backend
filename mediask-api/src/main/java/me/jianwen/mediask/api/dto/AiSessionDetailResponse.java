package me.jianwen.mediask.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionDetailResponse(
        Long sessionId,
        String sceneType,
        String status,
        Long departmentId,
        String chiefComplaintSummary,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<AiSessionTurnResponse> turns) {

    public record AiSessionTurnResponse(
            Long turnId,
            Integer turnNo,
            String turnStatus,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            Integer errorCode,
            String errorMessage,
            List<AiSessionMessageResponse> messages) {}

    public record AiSessionMessageResponse(String role, String content, OffsetDateTime createdAt) {}
}
