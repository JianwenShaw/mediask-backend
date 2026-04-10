package me.jianwen.mediask.api.dto;

public record ImportKnowledgeDocumentResponse(
        Long documentId, String documentUuid, Integer chunkCount, String documentStatus) {}
