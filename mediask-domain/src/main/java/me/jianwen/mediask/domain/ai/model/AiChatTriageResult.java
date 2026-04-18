package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiChatTriageResult(
        AiTriageStage triageStage,
        String chiefComplaintSummary,
        RiskLevel riskLevel,
        GuardrailAction guardrailAction,
        List<String> followUpQuestions,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiCitation> citations,
        AiExecutionMetadata executionMetadata) {

    public AiChatTriageResult {
        triageStage = Objects.requireNonNull(triageStage, "triageStage must not be null");
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        guardrailAction = Objects.requireNonNull(guardrailAction, "guardrailAction must not be null");
        followUpQuestions = followUpQuestions == null
                ? List.of()
                : followUpQuestions.stream()
                        .filter(question -> question != null && !question.isBlank())
                        .map(String::trim)
                        .toList();
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = ArgumentChecks.blankToNull(careAdvice);
        citations = citations == null ? List.of() : List.copyOf(citations);
        executionMetadata = executionMetadata == null ? AiExecutionMetadata.empty() : executionMetadata;
    }
}
