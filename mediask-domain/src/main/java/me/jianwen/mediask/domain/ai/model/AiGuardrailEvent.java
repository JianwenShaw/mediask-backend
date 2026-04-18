package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiGuardrailEvent(
        Long id,
        Long runId,
        RiskLevel riskLevel,
        GuardrailAction actionTaken,
        List<String> matchedRuleCodes,
        AiTriageStage triageStage,
        AiTriageCompletionReason triageCompletionReason,
        List<String> followUpQuestions,
        String chiefComplaintSummary,
        List<RecommendedDepartment> recommendedDepartments,
        String careAdvice,
        String inputHash,
        String outputHash) {

    public AiGuardrailEvent {
        id = ArgumentChecks.requirePositive(id, "id");
        runId = ArgumentChecks.requirePositive(runId, "runId");
        matchedRuleCodes = matchedRuleCodes == null ? List.of() : List.copyOf(matchedRuleCodes);
        triageStage = ArgumentChecks.requireNonNull(triageStage, "triageStage");
        followUpQuestions = followUpQuestions == null
                ? List.of()
                : followUpQuestions.stream()
                        .filter(question -> question != null && !question.isBlank())
                        .map(String::trim)
                        .toList();
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        careAdvice = ArgumentChecks.blankToNull(careAdvice);
        inputHash = ArgumentChecks.blankToNull(inputHash);
        outputHash = ArgumentChecks.blankToNull(outputHash);
    }

    public static AiGuardrailEvent create(
            Long runId,
            RiskLevel riskLevel,
            GuardrailAction actionTaken,
            List<String> matchedRuleCodes,
            AiTriageStage triageStage,
            AiTriageCompletionReason triageCompletionReason,
            List<String> followUpQuestions,
            String chiefComplaintSummary,
            List<RecommendedDepartment> recommendedDepartments,
            String careAdvice,
            String inputHash,
            String outputHash) {
        return new AiGuardrailEvent(
                SnowflakeIdGenerator.nextId(),
                runId,
                riskLevel,
                actionTaken,
                matchedRuleCodes,
                triageStage,
                triageCompletionReason,
                followUpQuestions,
                chiefComplaintSummary,
                recommendedDepartments,
                careAdvice,
                inputHash,
                outputHash);
    }
}
