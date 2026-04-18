package me.jianwen.mediask.domain.clinical.port;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;

public interface EncounterQueryRepository {

    List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status);

    Optional<EncounterDetail> findDetailByEncounterId(Long encounterId);

    Optional<EncounterAiSummary> findAiSummaryByEncounterId(Long encounterId);
}
