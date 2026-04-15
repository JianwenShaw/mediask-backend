package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AiSessionRegistrationHandoffResponse(
        Long sessionId,
        Long recommendedDepartmentId,
        String recommendedDepartmentName,
        String chiefComplaintSummary,
        String suggestedVisitType,
        String blockedReason,
        RegistrationQueryResponse registrationQuery) {

    public record RegistrationQueryResponse(
            Long departmentId,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateFrom,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateTo) {}
}
