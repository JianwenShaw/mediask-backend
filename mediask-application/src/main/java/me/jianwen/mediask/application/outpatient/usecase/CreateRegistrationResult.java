package me.jianwen.mediask.application.outpatient.usecase;

import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public record CreateRegistrationResult(Long registrationId, String orderNo, RegistrationStatus status) {}
