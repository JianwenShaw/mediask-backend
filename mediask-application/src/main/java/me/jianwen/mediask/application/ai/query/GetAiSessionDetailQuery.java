package me.jianwen.mediask.application.ai.query;

public record GetAiSessionDetailQuery(
        String requestId,
        Long patientUserId,
        String sessionId) {}
