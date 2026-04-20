package me.jianwen.mediask.application.outpatient.usecase;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public record CancelRegistrationResult(Long registrationId, RegistrationStatus status, OffsetDateTime cancelledAt) {
}
