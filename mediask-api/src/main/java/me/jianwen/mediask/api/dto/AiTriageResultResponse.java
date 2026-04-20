package me.jianwen.mediask.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record AiTriageResultResponse(
        String triageStage,
        String riskLevel,
        String guardrailAction,
        String nextAction,
        List<String> followUpQuestions,
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
