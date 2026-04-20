package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;

public record AiSessionRegistrationHandoffResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long sessionId,
        @JsonSerialize(using = ToStringSerializer.class) Long recommendedDepartmentId,
        String recommendedDepartmentName,
        String chiefComplaintSummary,
        String suggestedVisitType,
        String blockedReason,
        RegistrationQueryResponse registrationQuery) {

    public record RegistrationQueryResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate dateFrom,
            @JsonFormat(pattern = "yyyy-MM-dd")
            LocalDate dateTo) {}
}
