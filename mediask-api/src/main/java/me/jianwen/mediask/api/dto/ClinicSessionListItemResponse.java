package me.jianwen.mediask.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClinicSessionListItemResponse(
        Long clinicSessionId,
        Long departmentId,
        String departmentName,
        Long doctorId,
        String doctorName,
        LocalDate sessionDate,
        String periodCode,
        String clinicType,
        Integer remainingCount,
        BigDecimal fee) {
}
