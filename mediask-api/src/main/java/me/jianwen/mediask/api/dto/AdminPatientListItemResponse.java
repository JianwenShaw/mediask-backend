package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;

public record AdminPatientListItemResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long patientId,
        @JsonSerialize(using = ToStringSerializer.class) Long userId,
        String patientNo,
        String username,
        String displayName,
        String mobileMasked,
        String gender,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        String bloodType,
        String accountStatus) {
}
