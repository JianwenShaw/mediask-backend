package me.jianwen.mediask.application.ai.query;

import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListKnowledgeDocumentsQuery(Long knowledgeBaseId, PageQuery pageQuery) {

    public ListKnowledgeDocumentsQuery {
        ArgumentChecks.requirePositive(knowledgeBaseId, "knowledgeBaseId");
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }

    public static ListKnowledgeDocumentsQuery page(Long knowledgeBaseId, Long pageNum, Long pageSize) {
        return new ListKnowledgeDocumentsQuery(knowledgeBaseId, PageQuery.of(pageNum, pageSize));
    }
}
