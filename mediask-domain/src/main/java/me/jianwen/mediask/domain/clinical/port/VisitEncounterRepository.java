package me.jianwen.mediask.domain.clinical.port;

import java.time.OffsetDateTime;
import me.jianwen.mediask.domain.clinical.model.VisitEncounter;

public interface VisitEncounterRepository {

    void save(VisitEncounter visitEncounter);

    boolean cancelScheduledByRegistrationId(Long registrationId);

    boolean startScheduledByEncounterId(Long encounterId, OffsetDateTime startedAt);

    boolean completeInProgressByEncounterId(Long encounterId, OffsetDateTime endedAt);
}
