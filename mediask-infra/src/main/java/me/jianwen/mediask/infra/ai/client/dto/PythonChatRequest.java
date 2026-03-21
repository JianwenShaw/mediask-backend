package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PythonChatRequest(
        @JsonProperty("model_run_id") Long modelRunId,
        @JsonProperty("turn_id") Long turnId,
        @JsonProperty("session_uuid") String sessionUuid,
        @JsonProperty("department_id") Long departmentId,
        @JsonProperty("scene_type") String sceneType,
        String message,
        @JsonProperty("context_summary") String contextSummary,
        @JsonProperty("use_rag") boolean useRag,
        boolean stream) {
}
