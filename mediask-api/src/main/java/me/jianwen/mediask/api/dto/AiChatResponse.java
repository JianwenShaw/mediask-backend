package me.jianwen.mediask.api.dto;

public record AiChatResponse(Long sessionId, Long turnId, String answer, AiTriageResultResponse triageResult) {}
