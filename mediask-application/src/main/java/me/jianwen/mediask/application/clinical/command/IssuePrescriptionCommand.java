package me.jianwen.mediask.application.clinical.command;

public record IssuePrescriptionCommand(Long encounterId, Long doctorId) {

    public IssuePrescriptionCommand {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
    }
}
