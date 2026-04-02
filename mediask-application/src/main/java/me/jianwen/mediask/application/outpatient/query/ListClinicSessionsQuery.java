package me.jianwen.mediask.application.outpatient.query;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListClinicSessionsQuery(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {

    public ListClinicSessionsQuery {
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
    }
}
