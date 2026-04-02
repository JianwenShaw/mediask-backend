package me.jianwen.mediask.domain.outpatient.model;

import java.time.OffsetDateTime;

public record RegistrationListItem(
        Long registrationId,
        String orderNo,
        RegistrationStatus status,
        OffsetDateTime createdAt,
        Long sourceAiSessionId) {
}
