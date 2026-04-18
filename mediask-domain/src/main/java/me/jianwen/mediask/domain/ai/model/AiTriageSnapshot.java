package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiTriageSnapshot(
        AiTriageStage triageStage,
        AiTriageCompletionReason triageCompletionReason,
        String chiefComplaintSummary,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice) {

    public AiTriageSnapshot {
        triageStage = Objects.requireNonNull(triageStage, "triageStage must not be null");
        if (triageStage == AiTriageStage.COLLECTING) {
            throw new IllegalArgumentException("triageSnapshot does not support collecting stage");
        }
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = ArgumentChecks.blankToNull(careAdvice);
    }
}
