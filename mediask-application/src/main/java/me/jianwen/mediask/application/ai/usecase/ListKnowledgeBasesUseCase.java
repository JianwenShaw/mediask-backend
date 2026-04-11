package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.ListKnowledgeBasesQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListKnowledgeBasesUseCase {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public ListKnowledgeBasesUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional(readOnly = true)
    public PageData<KnowledgeBaseSummary> handle(ListKnowledgeBasesQuery query) {
        return knowledgeBaseRepository.pageByKeyword(query.keyword(), query.pageQuery());
    }
}
