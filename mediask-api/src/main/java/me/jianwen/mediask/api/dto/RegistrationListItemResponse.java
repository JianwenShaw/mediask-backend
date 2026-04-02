package me.jianwen.mediask.api.dto;

import java.time.OffsetDateTime;

public record RegistrationListItemResponse(
        Long registrationId,
        String orderNo,
        String status,
        OffsetDateTime createdAt,
        Long sourceAiSessionId) {
}
