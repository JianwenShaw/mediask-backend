package me.jianwen.mediask.application.ai.command;

public record ImportKnowledgeDocumentCommand(
        Long knowledgeBaseId, String title, String sourceType, String sourceUri, String inlineContent) {}
