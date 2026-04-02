package me.jianwen.mediask.domain.outpatient.port;

import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;

public interface RegistrationOrderRepository {

    void save(RegistrationOrder registrationOrder);
}
