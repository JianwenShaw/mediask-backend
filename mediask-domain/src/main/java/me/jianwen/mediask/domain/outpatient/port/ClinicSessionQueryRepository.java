package me.jianwen.mediask.domain.outpatient.port;

import java.time.LocalDate;
import java.util.List;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionListItem;
import me.jianwen.mediask.domain.outpatient.model.ClinicSessionSlotListItem;

public interface ClinicSessionQueryRepository {

    List<ClinicSessionListItem> listOpenSessions(Long departmentId, LocalDate dateFrom, LocalDate dateTo);

    List<ClinicSessionSlotListItem> listAvailableSlotsBySessionId(Long clinicSessionId);
}
