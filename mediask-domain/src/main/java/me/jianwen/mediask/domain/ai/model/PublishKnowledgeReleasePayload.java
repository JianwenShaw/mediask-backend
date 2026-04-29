package me.jianwen.mediask.domain.ai.model;

public record PublishKnowledgeReleasePayload(
        String knowledgeBaseId,
        String targetIndexVersionId) {
}
