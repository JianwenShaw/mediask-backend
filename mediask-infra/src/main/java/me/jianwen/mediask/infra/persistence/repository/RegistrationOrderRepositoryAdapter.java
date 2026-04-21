package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.RegistrationOrder;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderRepository;
import me.jianwen.mediask.infra.persistence.dataobject.RegistrationOrderDO;
import me.jianwen.mediask.infra.persistence.dataobject.StatusTransitionLogDO;
import me.jianwen.mediask.infra.persistence.mapper.RegistrationOrderMapper;
import me.jianwen.mediask.infra.persistence.mapper.StatusTransitionLogMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RegistrationOrderRepositoryAdapter implements RegistrationOrderRepository {

    private final RegistrationOrderMapper registrationOrderMapper;
    private final StatusTransitionLogMapper statusTransitionLogMapper;

    public RegistrationOrderRepositoryAdapter(
            RegistrationOrderMapper registrationOrderMapper, StatusTransitionLogMapper statusTransitionLogMapper) {
        this.registrationOrderMapper = registrationOrderMapper;
        this.statusTransitionLogMapper = statusTransitionLogMapper;
    }

    @Override
    public void save(RegistrationOrder registrationOrder) {
        RegistrationOrderDO dataObject = new RegistrationOrderDO();
        dataObject.setId(registrationOrder.registrationId());
        mapToDataObject(registrationOrder, dataObject);
        registrationOrderMapper.insert(dataObject);
        recordTransition("REGISTRATION_ORDER", registrationOrder.registrationId(), null, "CONFIRMED", "CREATE");
    }

    @Override
    public Optional<RegistrationOrder> findByRegistrationIdAndPatientId(Long registrationId, Long patientUserId) {
        RegistrationOrderDO dataObject = registrationOrderMapper.selectOne(Wrappers.lambdaQuery(RegistrationOrderDO.class)
                .eq(RegistrationOrderDO::getId, registrationId)
                .eq(RegistrationOrderDO::getPatientId, patientUserId)
                .isNull(RegistrationOrderDO::getDeletedAt));
        return Optional.ofNullable(dataObject).map(this::toDomain);
    }

    @Override
    public void update(RegistrationOrder registrationOrder) {
        RegistrationOrderDO existing = registrationOrderMapper.selectById(registrationOrder.registrationId());
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BizException(OutpatientErrorCode.REGISTRATION_NOT_FOUND);
        }

        RegistrationOrderDO dataObject = new RegistrationOrderDO();
        dataObject.setId(registrationOrder.registrationId());
        dataObject.setVersion(existing.getVersion());
        mapToDataObject(registrationOrder, dataObject);
        int updatedRows = registrationOrderMapper.updateById(dataObject);
        if (updatedRows == 0) {
            throw new BizException(OutpatientErrorCode.REGISTRATION_CANCEL_NOT_ALLOWED);
        }
        recordTransition(
                "REGISTRATION_ORDER",
                registrationOrder.registrationId(),
                existing.getOrderStatus(),
                registrationOrder.status().name(),
                "CANCEL");
    }

    @Override
    public boolean completeConfirmedByRegistrationId(Long registrationId) {
        RegistrationOrderDO existing = registrationOrderMapper.selectOne(Wrappers.lambdaQuery(RegistrationOrderDO.class)
                .eq(RegistrationOrderDO::getId, registrationId)
                .isNull(RegistrationOrderDO::getDeletedAt));
        if (existing == null || !"CONFIRMED".equals(existing.getOrderStatus())) {
            return false;
        }

        RegistrationOrderDO dataObject = new RegistrationOrderDO();
        dataObject.setId(existing.getId());
        dataObject.setVersion(existing.getVersion());
        dataObject.setOrderStatus("COMPLETED");
        if (registrationOrderMapper.updateById(dataObject) == 0) {
            return false;
        }
        recordTransition("REGISTRATION_ORDER", registrationId, "CONFIRMED", "COMPLETED", "COMPLETE_ENCOUNTER");
        return true;
    }

    private void mapToDataObject(RegistrationOrder registrationOrder, RegistrationOrderDO dataObject) {
        dataObject.setOrderNo(registrationOrder.orderNo());
        dataObject.setPatientId(registrationOrder.patientId());
        dataObject.setDoctorId(registrationOrder.doctorId());
        dataObject.setDepartmentId(registrationOrder.departmentId());
        dataObject.setSessionId(registrationOrder.sessionId());
        dataObject.setSlotId(registrationOrder.slotId());
        dataObject.setSourceAiSessionId(registrationOrder.sourceAiSessionId());
        dataObject.setOrderStatus(registrationOrder.status().name());
        dataObject.setFee(registrationOrder.fee());
        dataObject.setCancelledAt(registrationOrder.cancelledAt());
        dataObject.setCancellationReason(registrationOrder.cancellationReason());
    }

    private RegistrationOrder toDomain(RegistrationOrderDO dataObject) {
        return RegistrationOrder.reconstitute(
                dataObject.getId(),
                dataObject.getOrderNo(),
                dataObject.getPatientId(),
                dataObject.getDoctorId(),
                dataObject.getDepartmentId(),
                dataObject.getSessionId(),
                dataObject.getSlotId(),
                dataObject.getSourceAiSessionId(),
                RegistrationStatus.valueOf(dataObject.getOrderStatus()),
                dataObject.getFee(),
                dataObject.getCancelledAt(),
                dataObject.getCancellationReason());
    }

    private void recordTransition(Long entityId, String fromStatus, String toStatus, String action) {
        recordTransition("REGISTRATION_ORDER", entityId, fromStatus, toStatus, action);
    }

    private void recordTransition(String entityType, Long entityId, String fromStatus, String toStatus, String action) {
        StatusTransitionLogDO log = new StatusTransitionLogDO();
        log.setId(SnowflakeIdGenerator.nextId());
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorUserId(parseLong(MDC.get(RequestConstants.MDC_USER_ID)));
        log.setRequestId(MDC.get(RequestConstants.MDC_REQUEST_ID));
        log.setOccurredAt(OffsetDateTime.now());
        statusTransitionLogMapper.insert(log);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
