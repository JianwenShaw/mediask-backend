package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.OffsetDateTime;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.request.RequestConstants;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSessionDO;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSlotDO;
import me.jianwen.mediask.infra.persistence.dataobject.StatusTransitionLogDO;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotReservationRow;
import me.jianwen.mediask.infra.persistence.mapper.StatusTransitionLogMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class ClinicSlotReservationRepositoryAdapter implements ClinicSlotReservationRepository {

    private final ClinicSessionMapper clinicSessionMapper;
    private final ClinicSlotMapper clinicSlotMapper;
    private final StatusTransitionLogMapper statusTransitionLogMapper;

    public ClinicSlotReservationRepositoryAdapter(
            ClinicSessionMapper clinicSessionMapper,
            ClinicSlotMapper clinicSlotMapper,
            StatusTransitionLogMapper statusTransitionLogMapper) {
        this.clinicSessionMapper = clinicSessionMapper;
        this.clinicSlotMapper = clinicSlotMapper;
        this.statusTransitionLogMapper = statusTransitionLogMapper;
    }

    @Override
    public boolean existsOpenSession(Long sessionId) {
        return clinicSessionMapper.selectCount(Wrappers.lambdaQuery(ClinicSessionDO.class)
                        .eq(ClinicSessionDO::getId, sessionId)
                        .eq(ClinicSessionDO::getSessionStatus, "OPEN")
                        .isNull(ClinicSessionDO::getDeletedAt))
                > 0;
    }

    @Override
    public Optional<ClinicSlotReservation> reserveAvailableSlot(Long sessionId, Long slotId) {
        ClinicSlotReservationRow reservationRow = clinicSlotMapper.selectReservableSlot(sessionId, slotId);
        if (reservationRow == null) {
            return Optional.empty();
        }

        ClinicSlotDO slotToUpdate = new ClinicSlotDO();
        slotToUpdate.setId(reservationRow.getSlotId());
        slotToUpdate.setVersion(reservationRow.getSlotVersion());
        slotToUpdate.setSlotStatus("BOOKED");
        slotToUpdate.setRemainingCount(0);
        int updatedRows = clinicSlotMapper.updateById(slotToUpdate);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        recordTransition(reservationRow.getSlotId(), "AVAILABLE", "BOOKED", "RESERVE");

        return Optional.of(new ClinicSlotReservation(
                reservationRow.getSessionId(),
                reservationRow.getSlotId(),
                reservationRow.getDoctorId(),
                reservationRow.getDepartmentId(),
                reservationRow.getFee()));
    }

    @Override
    public boolean releaseReservedSlot(Long sessionId, Long slotId, String expectedCurrentStatus) {
        ClinicSlotDO existingSlot = clinicSlotMapper.selectOne(Wrappers.lambdaQuery(ClinicSlotDO.class)
                .eq(ClinicSlotDO::getId, slotId)
                .eq(ClinicSlotDO::getSessionId, sessionId)
                .isNull(ClinicSlotDO::getDeletedAt));
        if (existingSlot == null || !expectedCurrentStatus.equals(existingSlot.getSlotStatus())) {
            return false;
        }

        ClinicSlotDO slotToUpdate = new ClinicSlotDO();
        slotToUpdate.setId(existingSlot.getId());
        slotToUpdate.setVersion(existingSlot.getVersion());
        slotToUpdate.setSlotStatus("AVAILABLE");
        slotToUpdate.setRemainingCount(1);
        if (clinicSlotMapper.updateById(slotToUpdate) == 0) {
            return false;
        }
        recordTransition(slotId, expectedCurrentStatus, "AVAILABLE", "RELEASE");
        return true;
    }

    @Override
    public void refreshSessionRemainingCount(Long sessionId) {
        ClinicSessionDO existingSession = clinicSessionMapper.selectOne(Wrappers.lambdaQuery(ClinicSessionDO.class)
                .eq(ClinicSessionDO::getId, sessionId)
                .eq(ClinicSessionDO::getSessionStatus, "OPEN")
                .isNull(ClinicSessionDO::getDeletedAt));
        if (existingSession == null) {
            throw new BizException(OutpatientErrorCode.SESSION_NOT_FOUND);
        }

        ClinicSessionDO sessionToUpdate = new ClinicSessionDO();
        sessionToUpdate.setId(existingSession.getId());
        sessionToUpdate.setVersion(existingSession.getVersion());
        sessionToUpdate.setRemainingCount(clinicSlotMapper.countAvailableSlots(sessionId));
        int updatedRows = clinicSessionMapper.updateById(sessionToUpdate);
        if (updatedRows == 0) {
            throw new BizException(OutpatientErrorCode.SESSION_UPDATE_CONFLICT);
        }
    }

    private void recordTransition(Long entityId, String fromStatus, String toStatus, String action) {
        StatusTransitionLogDO log = new StatusTransitionLogDO();
        log.setId(SnowflakeIdGenerator.nextId());
        log.setEntityType("CLINIC_SLOT");
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
