package me.jianwen.mediask.infra.ai.client.dto;

import java.util.UUID;

public record PythonKnowledgePrepareRequest(
        Long documentId,
        UUID documentUuid,
        Long knowledgeBaseId,
        String title,
        String sourceType,
        String sourceUri) {}
