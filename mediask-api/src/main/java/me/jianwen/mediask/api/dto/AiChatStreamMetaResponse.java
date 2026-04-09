package me.jianwen.mediask.api.dto;

public record AiChatStreamMetaResponse(Long sessionId, Long turnId, AiTriageResultResponse triageResult) {}
