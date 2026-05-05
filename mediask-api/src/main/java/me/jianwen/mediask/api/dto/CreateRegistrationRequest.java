package me.jianwen.mediask.api.dto;

public record CreateRegistrationRequest(Long clinicSessionId, Long clinicSlotId, String sourceAiSessionId) {}
