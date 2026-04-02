package me.jianwen.mediask.application.outpatient.usecase;

import java.util.List;
import me.jianwen.mediask.application.outpatient.query.ListClinicSessionsQuery;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListClinicSessionsUseCase {

    private final ClinicSessionQueryRepository clinicSessionQueryRepository;

    public ListClinicSessionsUseCase(ClinicSessionQueryRepository clinicSessionQueryRepository) {
        this.clinicSessionQueryRepository = clinicSessionQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<ClinicSessionListItem> handle(ListClinicSessionsQuery query) {
        return clinicSessionQueryRepository.listOpenSessions(query.departmentId(), query.dateFrom(), query.dateTo());
    }
}
