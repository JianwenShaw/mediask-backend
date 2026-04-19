package me.jianwen.mediask.api.dto;

public record RegistrationListItemResponse(
        Long registrationId,
        String orderNo,
        String status,
        String createdAt,
        Long sourceAiSessionId) {
}
