package me.jianwen.mediask.api.dto;

public record EncounterDetailResponse(
        Long encounterId, Long registrationId, EncounterPatientSummaryResponse patientSummary) {
}
