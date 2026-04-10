package me.jianwen.mediask.domain.ai.port;

import java.util.Optional;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;

public interface KnowledgeDocumentRepository {

    void save(KnowledgeDocument knowledgeDocument);

    Optional<KnowledgeDocument> findById(Long documentId);

    boolean existsEffectiveByKnowledgeBaseIdAndContentHash(Long knowledgeBaseId, String contentHash);

    void update(KnowledgeDocument knowledgeDocument);
}
