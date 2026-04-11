package me.jianwen.mediask.domain.ai.port;

import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;

public interface KnowledgeBaseRepository {

    boolean existsEnabled(Long knowledgeBaseId);

    PageData<KnowledgeBaseSummary> pageByKeyword(String keyword, PageQuery pageQuery);

    void save(KnowledgeBase knowledgeBase);

    Optional<KnowledgeBase> findById(Long knowledgeBaseId);

    Optional<KnowledgeBaseSummary> findSummaryById(Long knowledgeBaseId);

    void update(KnowledgeBase knowledgeBase);

    void deleteById(Long knowledgeBaseId);
}
