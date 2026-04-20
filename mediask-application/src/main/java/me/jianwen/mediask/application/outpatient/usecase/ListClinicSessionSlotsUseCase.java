package me.jianwen.mediask.application.outpatient.usecase;

import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionSlotListItem;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListClinicSessionSlotsUseCase {

    private final ClinicSessionQueryRepository clinicSessionQueryRepository;

    public ListClinicSessionSlotsUseCase(ClinicSessionQueryRepository clinicSessionQueryRepository) {
        this.clinicSessionQueryRepository = clinicSessionQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicSessionSlotListItem> handle(Long clinicSessionId) {
        return clinicSessionQueryRepository.listAvailableSlotsBySessionId(clinicSessionId);
    }
}
