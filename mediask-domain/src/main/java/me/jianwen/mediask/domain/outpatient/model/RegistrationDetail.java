package me.jianwen.mediask.domain.outpatient.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RegistrationDetail(
        Long registrationId,
        Long patientUserId,
        String orderNo,
        RegistrationStatus status,
        OffsetDateTime createdAt,
        Long sourceAiSessionId,
        Long clinicSessionId,
        Long clinicSlotId,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        LocalDate sessionDate,
        ClinicSessionPeriodCode periodCode,
        BigDecimal fee,
        OffsetDateTime cancelledAt,
        String cancellationReason) {
}
