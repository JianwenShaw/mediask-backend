package me.jianwen.mediask.domain.clinical.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EncounterPatientSummary(
        Long patientUserId,
        String patientName,
        Long departmentId,
        String departmentName,
        LocalDate sessionDate,
        String periodCode,
        VisitEncounterStatus encounterStatus,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt) {
}
