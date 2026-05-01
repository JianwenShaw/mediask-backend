package me.jianwen.mediask.domain.ai.model;

import java.util.List;

public record AiTriageResult(
        String triageStage,
        String triageCompletionReason,
        String nextAction,
        String riskLevel,
        String chiefComplaintSummary,
        List<String> followUpQuestions,
        List<AiTriageRecommendedDepartment> recommendedDepartments,
        String careAdvice,
        String blockedReason,
        String catalogVersion,
        List<AiTriageCitation> citations) {

    public boolean isCollecting() {
        return "COLLECTING".equals(triageStage);
    }

    public boolean isReady() {
        return "READY".equals(triageStage);
    }

    public boolean isBlocked() {
        return "BLOCKED".equals(triageStage);
    }
}
