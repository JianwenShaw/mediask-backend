package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionTriageResultView(
        Long sessionId,
        Long patientId,
        String chiefComplaintSummary,
        RiskLevel riskLevel,
        GuardrailAction guardrailAction,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiCitation> citations) {

    public AiSessionTriageResultView {
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        guardrailAction = Objects.requireNonNull(guardrailAction, "guardrailAction must not be null");
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = ArgumentChecks.blankToNull(careAdvice);
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
