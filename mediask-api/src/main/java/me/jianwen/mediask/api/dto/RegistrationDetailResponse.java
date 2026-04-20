package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RegistrationDetailResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long registrationId,
        String orderNo,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime createdAt,
        @JsonSerialize(using = ToStringSerializer.class) Long sourceAiSessionId,
        @JsonSerialize(using = ToStringSerializer.class) Long clinicSessionId,
        @JsonSerialize(using = ToStringSerializer.class) Long clinicSlotId,
        @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
        String departmentName,
        @JsonSerialize(using = ToStringSerializer.class) Long doctorId,
        String doctorName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate,
        String periodCode,
        BigDecimal fee,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime cancelledAt,
        String cancellationReason) {
}
