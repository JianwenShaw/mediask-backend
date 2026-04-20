package me.jianwen.mediask.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistrationDetailResponse(
        Long registrationId,
        String orderNo,
        String status,
        String createdAt,
        Long sourceAiSessionId,
        Long clinicSessionId,
        Long clinicSlotId,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        LocalDate sessionDate,
        String periodCode,
        BigDecimal fee,
        String cancelledAt,
        String cancellationReason) {
}
