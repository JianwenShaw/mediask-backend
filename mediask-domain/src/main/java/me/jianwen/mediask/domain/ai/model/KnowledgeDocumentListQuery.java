package me.jianwen.mediask.domain.ai.model;

public record KnowledgeDocumentListQuery(
        String knowledgeBaseId,
        Integer pageNum,
        Integer pageSize) {
}
