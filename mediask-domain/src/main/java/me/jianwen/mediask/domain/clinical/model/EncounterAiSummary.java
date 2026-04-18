package me.jianwen.mediask.domain.clinical.model;

import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;

public record EncounterAiSummary(
        Long encounterId,
        Long sessionId,
        String chiefComplaintSummary,
        String structuredSummary,
        RiskLevel riskLevel,
        List<RecommendedDepartment> recommendedDepartments,
        List<AiCitation> latestCitations) {

    public EncounterAiSummary {
        encounterId = ArgumentChecks.requirePositive(encounterId, "encounterId");
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        structuredSummary = ArgumentChecks.blankToNull(structuredSummary);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        recommendedDepartments = recommendedDepartments == null ? List.of() : List.copyOf(recommendedDepartments);
        latestCitations = latestCitations == null ? List.of() : List.copyOf(latestCitations);
    }
}
