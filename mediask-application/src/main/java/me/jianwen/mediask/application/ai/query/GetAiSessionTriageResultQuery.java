package me.jianwen.mediask.application.ai.query;

public record GetAiSessionTriageResultQuery(
        String requestId,
        Long patientUserId,
        String sessionId) {}
