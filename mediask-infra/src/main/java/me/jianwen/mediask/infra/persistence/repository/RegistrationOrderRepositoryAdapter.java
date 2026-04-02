package me.jianwen.mediask.infra.persistence.repository;

import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import org.springframework.stereotype.Component;

@Component
public class RegistrationOrderRepositoryAdapter implements RegistrationOrderRepository {

    private final RegistrationOrderMapper registrationOrderMapper;

    public RegistrationOrderRepositoryAdapter(RegistrationOrderMapper registrationOrderMapper) {
        this.registrationOrderMapper = registrationOrderMapper;
    }

    @Override
    public void save(RegistrationOrder registrationOrder) {
        RegistrationOrderDO dataObject = new RegistrationOrderDO();
        dataObject.setId(registrationOrder.registrationId());
        dataObject.setOrderNo(registrationOrder.orderNo());
        dataObject.setPatientId(registrationOrder.patientId());
        dataObject.setDoctorId(registrationOrder.doctorId());
        dataObject.setDepartmentId(registrationOrder.departmentId());
        dataObject.setSessionId(registrationOrder.sessionId());
        dataObject.setSlotId(registrationOrder.slotId());
        dataObject.setSourceAiSessionId(registrationOrder.sourceAiSessionId());
        dataObject.setOrderStatus(registrationOrder.status().name());
        dataObject.setFee(registrationOrder.fee());
        registrationOrderMapper.insert(dataObject);
    }
}
