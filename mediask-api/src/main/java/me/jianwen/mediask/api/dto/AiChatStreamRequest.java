package me.jianwen.mediask.api.dto;

public record AiChatStreamRequest(
        Long sessionId, String message, Long departmentId, String sceneType, Boolean useStream) {}
