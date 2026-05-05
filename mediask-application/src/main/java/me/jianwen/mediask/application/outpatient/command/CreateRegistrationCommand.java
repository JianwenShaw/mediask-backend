package me.jianwen.mediask.application.outpatient.command;

public record CreateRegistrationCommand(
        Long patientUserId, Long clinicSessionId, Long clinicSlotId, String sourceAiSessionId) {}
