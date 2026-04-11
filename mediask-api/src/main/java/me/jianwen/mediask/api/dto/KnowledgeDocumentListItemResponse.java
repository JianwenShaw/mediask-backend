package me.jianwen.mediask.api.dto;

public record KnowledgeDocumentListItemResponse(
        Long id,
        String documentUuid,
        String title,
        String sourceType,
        String documentStatus,
        Long chunkCount) {}
