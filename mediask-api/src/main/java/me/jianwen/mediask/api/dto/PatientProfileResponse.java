package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;

public record PatientProfileResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long patientId,
        String patientNo,
        String gender,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {
}
