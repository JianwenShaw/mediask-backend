package me.jianwen.mediask.infra.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionSlotListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicType;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionListRow;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionSlotListRow;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSlotMapper;
import org.springframework.stereotype.Component;

@Component
public class ClinicSessionQueryRepositoryAdapter implements ClinicSessionQueryRepository {

    private final ClinicSessionMapper clinicSessionMapper;
    private final ClinicSlotMapper clinicSlotMapper;

    public ClinicSessionQueryRepositoryAdapter(ClinicSessionMapper clinicSessionMapper, ClinicSlotMapper clinicSlotMapper) {
        this.clinicSessionMapper = clinicSessionMapper;
        this.clinicSlotMapper = clinicSlotMapper;
    }

    @Override
    public List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
        return clinicSessionMapper.selectOpenClinicSessions(departmentId, dateFrom, dateTo).stream()
                .map(this::toListItem)
                .toList();
    }

    @Override
    public List<ClinicSessionSlotListItem> listAvailableSlotsBySessionId(Long clinicSessionId) {
        return clinicSlotMapper.selectAvailableSlotsBySessionId(clinicSessionId).stream()
                .map(this::toSlotListItem)
                .toList();
    }

    private ClinicSessionListItem toListItem(ClinicSessionListRow row) {
        return new ClinicSessionListItem(
                row.getClinicSessionId(),
                row.getDepartmentId(),
                row.getDepartmentName(),
                row.getDoctorId(),
                row.getDoctorName(),
                row.getSessionDate(),
                ClinicSessionPeriodCode.valueOf(row.getPeriodCode()),
                ClinicType.valueOf(row.getClinicType()),
                row.getRemainingCount(),
                row.getFee());
    }

    private ClinicSessionSlotListItem toSlotListItem(ClinicSessionSlotListRow row) {
        return new ClinicSessionSlotListItem(
                row.getClinicSlotId(), row.getSlotSeq(), row.getSlotStartTime(), row.getSlotEndTime());
    }
}
