package me.jianwen.mediask.application.ai.command;

public record SubmitAiTriageQueryCommand(
        String requestId,
        Long patientUserId,
        String sessionId,
        String hospitalScope,
        String userMessage) {}
