package me.jianwen.mediask.application.ai.usecase;

public record ImportKnowledgeDocumentResult(
        Long documentId, String documentUuid, Integer chunkCount, String documentStatus) {}
