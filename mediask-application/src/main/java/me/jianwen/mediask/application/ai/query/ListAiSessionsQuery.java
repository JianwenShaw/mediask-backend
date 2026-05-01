package me.jianwen.mediask.application.ai.query;

public record ListAiSessionsQuery(
        String requestId,
        Long patientUserId) {}
