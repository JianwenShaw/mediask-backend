package me.jianwen.mediask.application.ai.command;

public record ImportKnowledgeDocumentCommand(
        Long knowledgeBaseId, String originalFilename, String contentType, byte[] fileContent) {}
