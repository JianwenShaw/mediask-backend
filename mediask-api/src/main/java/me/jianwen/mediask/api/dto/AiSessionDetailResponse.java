package me.jianwen.mediask.api.dto;

import java.util.List;

public record AiSessionDetailResponse(
        Long sessionId,
        String sceneType,
        String status,
        Long departmentId,
        String chiefComplaintSummary,
        String summary,
        String startedAt,
        String endedAt,
        List<AiSessionTurnResponse> turns) {

    public record AiSessionTurnResponse(
            Long turnId,
            Integer turnNo,
            String turnStatus,
            String startedAt,
            String completedAt,
            Integer errorCode,
            String errorMessage,
            List<AiSessionMessageResponse> messages) {}

    public record AiSessionMessageResponse(String role, String content, String createdAt) {}
}
