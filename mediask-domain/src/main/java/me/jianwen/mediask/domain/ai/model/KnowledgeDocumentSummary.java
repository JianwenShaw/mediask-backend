package me.jianwen.mediask.domain.ai.model;

public record KnowledgeDocumentSummary(
        Long id,
        String documentUuid,
        String title,
        KnowledgeSourceType sourceType,
        KnowledgeDocumentStatus documentStatus,
        long chunkCount) {}
