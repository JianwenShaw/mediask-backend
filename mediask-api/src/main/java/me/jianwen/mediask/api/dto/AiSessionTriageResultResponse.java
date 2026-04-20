package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.OffsetDateTime;
import java.util.List;

public record AiSessionTriageResultResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long sessionId,
        String resultStatus,
        String triageStage,
        String riskLevel,
        String guardrailAction,
        String nextAction,
        @JsonSerialize(using = ToStringSerializer.class) Long finalizedTurnId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime finalizedAt,
        boolean hasActiveCycle,
        Integer activeCycleTurnNo,
        String chiefComplaintSummary,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        String careAdvice,
        List<CitationResponse> citations) {

    public record RecommendedDepartmentResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long departmentId,
            String departmentName,
            Integer priority,
            String reason) {}

    public record CitationResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long chunkId,
            Integer retrievalRank,
            Double fusionScore,
            String snippet) {}
}
