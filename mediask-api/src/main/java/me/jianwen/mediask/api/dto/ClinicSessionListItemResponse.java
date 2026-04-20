package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClinicSessionListItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long clinicSessionId,
        @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
        String departmentName,
        @JsonSerialize(using = ToStringSerializer.class) Long doctorId,
        String doctorName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate,
        String periodCode,
        String clinicType,
        Integer remainingCount,
        BigDecimal fee) {
}
