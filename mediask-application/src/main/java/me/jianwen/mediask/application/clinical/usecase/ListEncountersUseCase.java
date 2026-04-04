package me.jianwen.mediask.application.clinical.usecase;

import java.util.List;
import me.jianwen.mediask.application.clinical.query.ListEncountersQuery;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListEncountersUseCase {

    private final EncounterQueryRepository encounterQueryRepository;

    public ListEncountersUseCase(EncounterQueryRepository encounterQueryRepository) {
        this.encounterQueryRepository = encounterQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<EncounterListItem> handle(ListEncountersQuery query) {
        return encounterQueryRepository.listByDoctorId(query.doctorId(), query.status());
    }
}
