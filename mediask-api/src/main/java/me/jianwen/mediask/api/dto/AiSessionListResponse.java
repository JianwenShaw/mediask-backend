package me.jianwen.mediask.api.dto;

import java.util.List;

public record AiSessionListResponse(List<AiSessionListItemResponse> items) {

    public record AiSessionListItemResponse(
            Long sessionId,
            String sceneType,
            String status,
            Long departmentId,
            String chiefComplaintSummary,
            String summary,
            String startedAt,
            String endedAt) {}
}
