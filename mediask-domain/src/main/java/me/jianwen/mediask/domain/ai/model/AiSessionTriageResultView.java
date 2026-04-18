package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionTriageResultView(
        Long sessionId,
        Long patientId,
        AiTriageResultStatus resultStatus,
        AiTriageStage triageStage,
        Long finalizedTurnId,
        OffsetDateTime finalizedAt,
        boolean hasActiveCycle,
        Integer activeCycleTurnNo,
        String chiefComplaintSummary,
        RiskLevel riskLevel,
        GuardrailAction guardrailAction,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiCitation> citations) {

    public AiSessionTriageResultView {
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        resultStatus = Objects.requireNonNull(resultStatus, "resultStatus must not be null");
        triageStage = Objects.requireNonNull(triageStage, "triageStage must not be null");
        finalizedTurnId = ArgumentChecks.requirePositive(finalizedTurnId, "finalizedTurnId");
        finalizedAt = ArgumentChecks.requireNonNull(finalizedAt, "finalizedAt");
        activeCycleTurnNo = hasActiveCycle
                ? ArgumentChecks.requirePositive(activeCycleTurnNo == null ? null : Long.valueOf(activeCycleTurnNo), "activeCycleTurnNo")
                        .intValue()
                : null;
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        guardrailAction = Objects.requireNonNull(guardrailAction, "guardrailAction must not be null");
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = ArgumentChecks.blankToNull(careAdvice);
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
