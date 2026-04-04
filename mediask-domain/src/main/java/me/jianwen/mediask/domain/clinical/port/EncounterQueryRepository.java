package me.jianwen.mediask.domain.clinical.port;

import java.util.List;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;

public interface EncounterQueryRepository {

    List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status);
}
