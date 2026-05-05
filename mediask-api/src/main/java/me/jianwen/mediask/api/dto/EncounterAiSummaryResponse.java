package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.OffsetDateTime;
import java.util.List;

public record EncounterAiSummaryResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long encounterId,
        String sessionId,
        String chiefComplaintSummary,
        String riskLevel,
        List<RecommendedDepartmentResponse> recommendedDepartments,
        String careAdvice,
        List<CitationResponse> citations,
        String blockedReason,
        String catalogVersion,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime finalizedAt) {

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
