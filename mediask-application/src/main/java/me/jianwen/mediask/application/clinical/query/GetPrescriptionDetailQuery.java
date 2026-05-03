package me.jianwen.mediask.application.clinical.query;

public record GetPrescriptionDetailQuery(Long encounterId, Long doctorId, Long patientUserId) {

    public GetPrescriptionDetailQuery(Long encounterId, Long doctorId) {
        this(encounterId, doctorId, null);
    }

    public GetPrescriptionDetailQuery {
        if (encounterId == null) {
            throw new IllegalArgumentException("encounterId cannot be null");
        }
        if (doctorId == null && patientUserId == null) {
            throw new IllegalArgumentException("doctorId and patientUserId cannot both be null");
        }
    }
}
