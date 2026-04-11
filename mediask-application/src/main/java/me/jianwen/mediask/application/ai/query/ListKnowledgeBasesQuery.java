package me.jianwen.mediask.application.ai.query;

import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListKnowledgeBasesQuery(String keyword, PageQuery pageQuery) {

    public ListKnowledgeBasesQuery {
        keyword = ArgumentChecks.blankToNull(keyword);
        if (pageQuery == null) {
            throw new IllegalArgumentException("pageQuery must not be null");
        }
    }

    public static ListKnowledgeBasesQuery page(String keyword, Long pageNum, Long pageSize) {
        return new ListKnowledgeBasesQuery(keyword, PageQuery.of(pageNum, pageSize));
    }
}
