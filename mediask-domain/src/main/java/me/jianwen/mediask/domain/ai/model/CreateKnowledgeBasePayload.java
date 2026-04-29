package me.jianwen.mediask.domain.ai.model;

public record CreateKnowledgeBasePayload(
        String code,
        String name,
        String description,
        String defaultEmbeddingModel,
        Integer defaultEmbeddingDimension,
        String retrievalStrategy) {
}
