package me.jianwen.mediask.application.clinical.usecase;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;

public record UpdateEncounterStatusResult(
        Long encounterId, VisitEncounterStatus encounterStatus, OffsetDateTime startedAt, OffsetDateTime endedAt) {}
