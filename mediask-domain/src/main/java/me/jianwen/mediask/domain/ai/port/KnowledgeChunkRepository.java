package me.jianwen.mediask.domain.ai.port;

import java.util.List;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;

public interface KnowledgeChunkRepository {

    void saveAll(List<KnowledgeChunk> knowledgeChunks);
}
