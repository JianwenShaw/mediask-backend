package me.jianwen.mediask.domain.ai.model;

import java.util.Objects;

public record AiChatInvocation(
        Long modelRunId,
        Long turnId,
        String sessionUuid,
        String message,
        AiSceneType sceneType,
        Long departmentId,
        String contextSummary,
        boolean useRag) {

    public AiChatInvocation {
        modelRunId = requirePositive(modelRunId, "modelRunId");
        turnId = requirePositive(turnId, "turnId");
        sessionUuid = requireNonBlank(sessionUuid, "sessionUuid");
        message = requireNonBlank(message, "message");
        sceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        departmentId = normalizePositive(departmentId, "departmentId");
        contextSummary = normalizeBlank(contextSummary);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static Long normalizePositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
