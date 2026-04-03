package me.jianwen.mediask.application.outpatient.usecase;

import java.util.List;
import me.jianwen.mediask.application.outpatient.query.ListRegistrationsQuery;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListRegistrationsUseCase {

    private final RegistrationOrderQueryRepository registrationOrderQueryRepository;

    public ListRegistrationsUseCase(RegistrationOrderQueryRepository registrationOrderQueryRepository) {
        this.registrationOrderQueryRepository = registrationOrderQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationListItem> handle(ListRegistrationsQuery query) {
        return registrationOrderQueryRepository.listByPatientUserId(query.patientUserId(), query.status());
    }
}
