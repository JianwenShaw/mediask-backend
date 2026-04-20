package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record DoctorProfileResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long doctorId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        @JsonSerialize(using = ToStringSerializer.class) Long hospitalId,
        @JsonSerialize(using = ToStringSerializer.class) Long primaryDepartmentId,
        String primaryDepartmentName) {
}
