package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PythonChatResponse(
        @JsonProperty("model_run_id") Long modelRunId,
        @JsonProperty("provider_run_id") String providerRunId,
        String answer,
        String summary,
        @JsonProperty("chief_complaint_summary") String chiefComplaintSummary,
        @JsonProperty("recommended_departments") List<PythonRecommendedDepartment> recommendedDepartments,
        @JsonProperty("care_advice") String careAdvice,
        List<PythonCitation> citations,
        @JsonProperty("risk_level") String riskLevel,
        @JsonProperty("guardrail_action") String guardrailAction,
        @JsonProperty("matched_rule_codes") List<String> matchedRuleCodes,
        @JsonProperty("tokens_input") Integer tokensInput,
        @JsonProperty("tokens_output") Integer tokensOutput,
        @JsonProperty("latency_ms") Integer latencyMs,
        @JsonProperty("is_degraded") Boolean degraded) {

    public record PythonRecommendedDepartment(
            @JsonProperty("department_id") Long departmentId,
            @JsonProperty("department_name") String departmentName,
            Integer priority,
            String reason) {
    }

    public record PythonCitation(
            @JsonProperty("chunk_id") Long chunkId,
            @JsonProperty("retrieval_rank") Integer retrievalRank,
            @JsonProperty("fusion_score") Double fusionScore,
            String snippet) {
    }
}
