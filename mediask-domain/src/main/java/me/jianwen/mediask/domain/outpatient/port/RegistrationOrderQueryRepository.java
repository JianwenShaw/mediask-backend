package me.jianwen.mediask.domain.outpatient.port;

import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;

public interface RegistrationOrderQueryRepository {

    List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status);

    Optional<RegistrationDetail> findDetailByPatientUserIdAndRegistrationId(Long patientUserId, Long registrationId);
}
