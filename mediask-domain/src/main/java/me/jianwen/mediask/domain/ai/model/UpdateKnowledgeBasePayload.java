package me.jianwen.mediask.domain.ai.model;

public record UpdateKnowledgeBasePayload(
        String name,
        String description,
        String status) {
}
