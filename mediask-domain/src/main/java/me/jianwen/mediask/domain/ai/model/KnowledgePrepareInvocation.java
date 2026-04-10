package me.jianwen.mediask.domain.ai.model;

import java.util.UUID;

public record KnowledgePrepareInvocation(
        Long documentId,
        UUID documentUuid,
        Long knowledgeBaseId,
        String title,
        KnowledgeSourceType sourceType,
        String sourceUri,
        String inlineContent) {}
