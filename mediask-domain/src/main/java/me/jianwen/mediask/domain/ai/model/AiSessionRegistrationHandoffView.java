package me.jianwen.mediask.domain.ai.model;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionRegistrationHandoffView(
        Long sessionId,
        Long patientId,
        Long recommendedDepartmentId,
        String recommendedDepartmentName,
        String chiefComplaintSummary,
        String suggestedVisitType,
        String blockedReason,
        RegistrationQuery registrationQuery) {

    public AiSessionRegistrationHandoffView {
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        recommendedDepartmentName = ArgumentChecks.blankToNull(recommendedDepartmentName);
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        suggestedVisitType = ArgumentChecks.blankToNull(suggestedVisitType);
        blockedReason = ArgumentChecks.blankToNull(blockedReason);
    }

    public record RegistrationQuery(Long departmentId, LocalDate dateFrom, LocalDate dateTo) {

        public RegistrationQuery {
            departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
            dateFrom = ArgumentChecks.requireNonNull(dateFrom, "dateFrom");
            dateTo = ArgumentChecks.requireNonNull(dateTo, "dateTo");
        }
    }
}
