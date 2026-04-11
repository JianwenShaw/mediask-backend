package me.jianwen.mediask.domain.ai.port;

import java.util.Optional;
import me.jianwen.mediask.common.pagination.PageData;
import me.jianwen.mediask.common.pagination.PageQuery;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocumentSummary;

public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocument knowledgeDocument);

    Optional<KnowledgeDocument> findById(Long documentId);

    boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash);

    void update(KnowledgeDocument knowledgeDocument);

    PageData<KnowledgeDocumentSummary> pageByKnowledgeBaseId(Long knowledgeBaseId, PageQuery pageQuery);

    void deleteById(Long documentId);
}
