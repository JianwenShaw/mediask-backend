package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record EncounterPatientSummaryResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long patientUserId,
        String patientName,
        String gender,
        @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
        String departmentName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate sessionDate,
        String periodCode,
        String encounterStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime startedAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime endedAt,
        Integer age) {
}
