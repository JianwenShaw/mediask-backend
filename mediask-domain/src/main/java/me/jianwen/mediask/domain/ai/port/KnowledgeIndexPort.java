package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;

public interface KnowledgeIndexPort {

    void index(KnowledgeDocument knowledgeDocument);
}
