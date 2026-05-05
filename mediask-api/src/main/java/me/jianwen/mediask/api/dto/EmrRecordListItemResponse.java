package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EmrRecordListItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long emrRecordId,
        String recordNo,
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        String recordStatus,
        @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
        String departmentName,
        @JsonSerialize(using = ToStringSerializer.class) Long doctorId,
        String doctorName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate,
        String chiefComplaintSummary,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime createdAt) {
}
