package me.jianwen.mediask.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EncounterPatientSummaryResponse(
        Long patientUserId,
        String patientName,
        Long departmentId,
        String departmentName,
        LocalDate sessionDate,
        String periodCode,
        String encounterStatus,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
