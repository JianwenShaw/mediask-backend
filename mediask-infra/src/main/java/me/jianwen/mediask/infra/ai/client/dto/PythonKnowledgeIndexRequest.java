package me.jianwen.mediask.infra.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PythonKnowledgeIndexRequest(
        @JsonProperty("document_id") Long documentId,
        @JsonProperty("knowledge_base_id") Long knowledgeBaseId) {}
