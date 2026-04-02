package me.jianwen.mediask.application.outpatient.command;

public record CreateRegistrationCommand(
        Long patientId, Long clinicSessionId, Long clinicSlotId, Long sourceAiSessionId) {}
