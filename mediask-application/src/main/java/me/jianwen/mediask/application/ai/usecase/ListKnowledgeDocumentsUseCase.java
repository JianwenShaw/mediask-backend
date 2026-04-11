package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.ListKnowledgeDocumentsQuery;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import org.springframework.transaction.annotation.Transactional;

public class ListKnowledgeDocumentsUseCase {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public ListKnowledgeDocumentsUseCase(KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    @Transactional(readOnly = true)
    public PageData<KnowledgeDocumentSummary> handle(ListKnowledgeDocumentsQuery query) {
        return knowledgeDocumentRepository.pageByKnowledgeBaseId(query.knowledgeBaseId(), query.pageQuery());
    }
}
