package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionTriageResultResponse(
        String sessionId,
        String resultStatus,
        String triageStage,
        String riskLevel,
        String guardrailAction,
        String nextAction,
        String finalizedTurnId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime finalizedAt,
        Boolean hasActiveCycle,
        Integer activeCycleTurnNo,
        String chiefComplaintSummary,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        String careAdvice,
        List<CitationResponse> citations,
        String blockedReason,
        String catalogVersion) {

    public record RecommendedDepartmentResponse(
            String departmentId,
            String departmentName,
            Integer priority,
            String reason) {}

    public record CitationResponse(
            Integer citationOrder,
            String chunkId,
            String snippet) {}
}
