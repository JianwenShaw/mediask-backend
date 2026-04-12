package me.jianwen.mediask.api.dto;

public record AiChatRequest(Long sessionId, String message, Long departmentId, String sceneType, Boolean useStream) {}
