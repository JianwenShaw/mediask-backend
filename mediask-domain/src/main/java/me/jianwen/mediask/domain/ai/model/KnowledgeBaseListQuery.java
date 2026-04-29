package me.jianwen.mediask.domain.ai.model;

public record KnowledgeBaseListQuery(
        String keyword,
        Integer pageNum,
        Integer pageSize) {
}
