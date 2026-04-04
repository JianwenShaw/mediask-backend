package me.jianwen.mediask.application.clinical.query;

import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;

public record ListEncountersQuery(Long doctorId, VisitEncounterStatus status) {
}
