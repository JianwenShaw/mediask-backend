package me.jianwen.mediask.domain.clinical.model;

import me.jianwen.mediask.common.id.SnowflakeIdGenerator;

public record VisitEncounter(
        Long encounterId,
        Long registrationId,
        Long patientUserId,
        Long doctorId,
        Long departmentId,
        VisitEncounterStatus status) {

    public static VisitEncounter createScheduled(
            Long registrationId, Long patientUserId, Long doctorId, Long departmentId) {
        return new VisitEncounter(
                SnowflakeIdGenerator.nextId(),
                registrationId,
                patientUserId,
                doctorId,
                departmentId,
                VisitEncounterStatus.SCHEDULED);
    }
}
