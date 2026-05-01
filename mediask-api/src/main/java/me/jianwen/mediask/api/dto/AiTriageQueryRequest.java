package me.jianwen.mediask.api.dto;

public record AiTriageQueryRequest(
        String sessionId,
        String hospitalScope,
        String userMessage) {}
