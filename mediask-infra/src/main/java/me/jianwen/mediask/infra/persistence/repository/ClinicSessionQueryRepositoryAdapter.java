package me.jianwen.mediask.infra.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionPeriodCode;
import me.jianwen.mediask.domain.outpatient.model.ClinicType;
import me.jianwen.mediask.domain.outpatient.port.ClinicSessionQueryRepository;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionListRow;
import me.jianwen.mediask.infra.persistence.mapper.ClinicSessionMapper;
import org.springframework.stereotype.Component;

@Component
public class ClinicSessionQueryRepositoryAdapter implements ClinicSessionQueryRepository {

    private final ClinicSessionMapper clinicSessionMapper;

    public ClinicSessionQueryRepositoryAdapter(ClinicSessionMapper clinicSessionMapper) {
        this.clinicSessionMapper = clinicSessionMapper;
    }

    @Override
    public List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {
        return clinicSessionMapper.selectOpenClinicSessions(departmentId, dateFrom, dateTo).stream()
                .map(this::toListItem)
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
}
