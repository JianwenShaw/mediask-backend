package me.jianwen.mediask.api.dto;

public record ImportKnowledgeDocumentRequest(
        Long knowledgeBaseId, String title, String sourceType, String sourceUri, String inlineContent) {}
