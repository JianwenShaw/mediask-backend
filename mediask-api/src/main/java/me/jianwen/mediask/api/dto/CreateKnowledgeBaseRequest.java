package me.jianwen.mediask.api.dto;

public record CreateKnowledgeBaseRequest(
        String code,
        String name,
        String description,
        String defaultEmbeddingModel,
        Integer defaultEmbeddingDimension,
        String retrievalStrategy) {
}
