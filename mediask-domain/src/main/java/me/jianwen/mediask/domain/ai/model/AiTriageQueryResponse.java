package me.jianwen.mediask.domain.ai.model;

public record AiTriageQueryResponse(
        String requestId,
        String sessionId,
        String turnId,
        String queryRunId,
        AiTriageResult triageResult) {}
