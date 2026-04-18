package me.jianwen.mediask.domain.clinical.model;

public record EncounterDetail(Long encounterId, Long registrationId, Long doctorId, EncounterPatientSummary patientSummary) {
}
