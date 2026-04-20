package me.jianwen.mediask.application.outpatient.command;

public record CancelRegistrationCommand(Long registrationId, Long patientUserId) {
}
