package me.jianwen.mediask.domain.outpatient.port;

import java.util.Optional;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;

public interface RegistrationOrderRepository {

    void save(RegistrationOrder registrationOrder);

    Optional<RegistrationOrder> findByRegistrationIdAndPatientId(Long registrationId, Long patientUserId);

    void update(RegistrationOrder registrationOrder);

    boolean completeConfirmedByRegistrationId(Long registrationId);
}
