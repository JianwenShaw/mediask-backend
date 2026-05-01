package me.jianwen.mediask.domain.ai.model;

import java.util.List;

public record AiTriageResultSnapshot(
        String requestId,
        String sessionId,
        String turnId,
        String queryRunId,
        String hospitalScope,
        String triageStage,
        String triageCompletionReason,
        String nextAction,
        String riskLevel,
        String chiefComplaintSummary,
        String careAdvice,
        String blockedReason,
        String catalogVersion,
        List<AiTriageRecommendedDepartment> recommendedDepartments,
        List<AiTriageCitation> citations) {}
