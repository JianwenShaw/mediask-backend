package me.jianwen.mediask.domain.clinical.model;

import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;

public record EncounterAiSummary(
        Long encounterId,
        Long patientUserId,
        String sessionId,
        String chiefComplaintSummary,
        String riskLevel,
        List<AiTriageRecommendedDepartment> recommendedDepartments,
        String careAdvice,
        List<AiTriageCitation> citations,
        String blockedReason,
        String catalogVersion,
        OffsetDateTime finalizedAt) {}
