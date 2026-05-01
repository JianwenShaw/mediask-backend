package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionDetailResponse(
        String sessionId,
        String sceneType,
        String status,
        @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
        String chiefComplaintSummary,
        String summary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime startedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime endedAt,
        List<TurnResponse> turns) {

    public record TurnResponse(
            String turnId,
            Integer turnNo,
            String turnStatus,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime startedAt,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime completedAt,
            String errorCode,
            String errorMessage,
            List<MessageResponse> messages) {}

    public record MessageResponse(
            String role,
            String content,
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
            OffsetDateTime createdAt) {}
}
