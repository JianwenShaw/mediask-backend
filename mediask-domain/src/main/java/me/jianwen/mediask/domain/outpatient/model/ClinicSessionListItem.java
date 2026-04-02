package me.jianwen.mediask.domain.outpatient.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ClinicSessionListItem(
        Long clinicSessionId,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        LocalDate sessionDate,
        ClinicSessionPeriodCode periodCode,
        ClinicType clinicType,
        Integer remainingCount,
        BigDecimal fee) {

    public ClinicSessionListItem {
        clinicSessionId = ArgumentChecks.requirePositive(clinicSessionId, "clinicSessionId");
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
        departmentName = ArgumentChecks.requireNonBlank(departmentName, "departmentName");
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
        doctorName = ArgumentChecks.requireNonBlank(doctorName, "doctorName");
        sessionDate = ArgumentChecks.requireNonNull(sessionDate, "sessionDate");
        periodCode = ArgumentChecks.requireNonNull(periodCode, "periodCode");
        clinicType = ArgumentChecks.requireNonNull(clinicType, "clinicType");
        remainingCount = ArgumentChecks.requireNonNull(remainingCount, "remainingCount");
        fee = ArgumentChecks.requireNonNull(fee, "fee");
    }
}
