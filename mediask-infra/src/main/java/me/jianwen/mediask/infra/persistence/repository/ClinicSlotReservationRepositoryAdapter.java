package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.outpatient.exception.OutpatientErrorCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSlotReservation;
import me.jianwen.mediask.domain.outpatient.port.ClinicSlotReservationRepository;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSessionDO;
import me.jianwen.mediask.infra.persistence.dataobject.ClinicSlotDO;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotReservationRow;
import org.springframework.stereotype.Component;

@Component
public class ClinicSlotReservationRepositoryAdapter implements ClinicSlotReservationRepository {

    private final ClinicSessionMapper clinicSessionMapper;
    private final ClinicSlotMapper clinicSlotMapper;

    public ClinicSlotReservationRepositoryAdapter(
            ClinicSessionMapper clinicSessionMapper, ClinicSlotMapper clinicSlotMapper) {
        this.clinicSessionMapper = clinicSessionMapper;
        this.clinicSlotMapper = clinicSlotMapper;
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
        slotToUpdate.setSlotStatus("LOCKED");
        slotToUpdate.setRemainingCount(0);
        int updatedRows = clinicSlotMapper.updateById(slotToUpdate);
        if (updatedRows == 0) {
            return Optional.empty();
        }

        return Optional.of(new ClinicSlotReservation(
                reservationRow.getSessionId(),
                reservationRow.getSlotId(),
                reservationRow.getDoctorId(),
                reservationRow.getDepartmentId(),
                reservationRow.getFee()));
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
}
