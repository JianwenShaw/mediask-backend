package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import java.util.Objects;

public record AiChatReply(
        String answer,
        String chiefComplaintSummary,
        RiskLevel riskLevel,
        GuardrailAction guardrailAction,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiCitation> citations,
        AiExecutionMetadata executionMetadata) {

    public AiChatReply {
        answer = requireNonBlank(answer, "answer");
        chiefComplaintSummary = normalizeBlank(chiefComplaintSummary);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        guardrailAction = Objects.requireNonNull(guardrailAction, "guardrailAction must not be null");
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = normalizeBlank(careAdvice);
        citations = citations == null ? List.of() : List.copyOf(citations);
        executionMetadata = executionMetadata == null ? AiExecutionMetadata.empty() : executionMetadata;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
