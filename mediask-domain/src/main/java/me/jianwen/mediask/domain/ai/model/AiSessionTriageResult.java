package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionTriageResult(
        String sessionId,
        String resultStatus,
        String triageStage,
        String riskLevel,
        String guardrailAction,
        String nextAction,
        String finalizedTurnId,
        OffsetDateTime finalizedAt,
        Boolean hasActiveCycle,
        Integer activeCycleTurnNo,
        String chiefComplaintSummary,
        List<AiTriageRecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiTriageCitation> citations,
        String blockedReason,
        String catalogVersion) {}
