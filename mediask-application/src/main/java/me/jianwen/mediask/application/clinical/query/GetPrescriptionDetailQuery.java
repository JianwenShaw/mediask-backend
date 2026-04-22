package me.jianwen.mediask.application.clinical.query;

public record GetPrescriptionDetailQuery(Long encounterId, Long doctorId) {

    public GetPrescriptionDetailQuery {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null) {
            throw new IllegalArgumentException("doctorId cannot be null");
        }
    }
}
