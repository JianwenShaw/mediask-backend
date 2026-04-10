package me.jianwen.mediask.domain.ai.port;

import java.util.List;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.model.KnowledgeDocument;

public interface KnowledgeIndexPort {

    void index(KnowledgeDocument knowledgeDocument, List<KnowledgeChunk> knowledgeChunks);
}
