package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PythonChatRequest(
        @JsonProperty("model_run_id") Long modelRunId,
        @JsonProperty("turn_id") Long turnId,
        @JsonProperty("session_uuid") String sessionUuid,
        @JsonProperty("department_id") Long departmentId,
        @JsonProperty("hospital_scope") String hospitalScope,
        @JsonProperty("department_catalog_version") String departmentCatalogVersion,
        @JsonProperty("patient_turn_no_in_active_cycle") Integer patientTurnNoInActiveCycle,
        @JsonProperty("force_finalize") boolean forceFinalize,
        @JsonProperty("scene_type") String sceneType,
        String message,
        @JsonProperty("context_summary") String contextSummary,
        @JsonProperty("use_rag") boolean useRag,
        @JsonProperty("knowledge_base_ids") List<Long> knowledgeBaseIds) {
}
