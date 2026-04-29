package me.jianwen.mediask.api.dto;

public record UpdateKnowledgeBaseRequest(
        String name,
        String description,
        String status) {
}
