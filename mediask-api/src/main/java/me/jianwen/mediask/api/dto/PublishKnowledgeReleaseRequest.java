package me.jianwen.mediask.api.dto;

public record PublishKnowledgeReleaseRequest(
        String knowledgeBaseId,
        String targetIndexVersionId) {
}
