package me.jianwen.mediask.api.dto;

import java.time.LocalDate;

public record EncounterListItemResponse(
        Long encounterId,
        Long registrationId,
        Long patientUserId,
        String patientName,
        Long departmentId,
        String departmentName,
        LocalDate sessionDate,
        String periodCode,
        String encounterStatus,
        String startedAt,
        String endedAt) {
}
