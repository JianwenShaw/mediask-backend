package me.jianwen.mediask.application.clinical.command;

public record CancelPrescriptionCommand(Long encounterId, Long doctorId) {

    public CancelPrescriptionCommand {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
    }
}
