package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record PythonKnowledgePrepareRequest(
        @JsonProperty("document_id") Long documentId,
        @JsonProperty("document_uuid") UUID documentUuid,
        @JsonProperty("knowledge_base_id") Long knowledgeBaseId,
        String title,
        @JsonProperty("source_type") String sourceType,
        @JsonProperty("source_uri") String sourceUri) {}
