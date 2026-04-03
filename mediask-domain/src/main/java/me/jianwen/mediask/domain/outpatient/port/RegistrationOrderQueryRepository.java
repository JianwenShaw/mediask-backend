package me.jianwen.mediask.domain.outpatient.port;

import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public interface RegistrationOrderQueryRepository {

    List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status);
}
