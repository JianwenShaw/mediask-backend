package me.jianwen.mediask.infra.persistence.repository;

import java.util.List;
import me.jianwen.mediask.domain.ai.model.KnowledgeChunk;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.infra.persistence.dataobject.KnowledgeChunkDO;
import me.jianwen.mediask.infra.persistence.mapper.KnowledgeChunkMapper;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeChunkRepositoryAdapter implements KnowledgeChunkRepository {

    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public KnowledgeChunkRepositoryAdapter(KnowledgeChunkMapper knowledgeChunkMapper) {
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    @Override
    public void saveAll(List<KnowledgeChunk> knowledgeChunks) {
        for (KnowledgeChunk knowledgeChunk : knowledgeChunks) {
            KnowledgeChunkDO dataObject = new KnowledgeChunkDO();
            dataObject.setId(knowledgeChunk.id());
            dataObject.setKnowledgeBaseId(knowledgeChunk.knowledgeBaseId());
            dataObject.setDocumentId(knowledgeChunk.documentId());
            dataObject.setChunkIndex(knowledgeChunk.chunkIndex());
            dataObject.setSectionTitle(knowledgeChunk.sectionTitle());
            dataObject.setPageNo(knowledgeChunk.pageNo());
            dataObject.setCharStart(knowledgeChunk.charStart());
            dataObject.setCharEnd(knowledgeChunk.charEnd());
            dataObject.setTokenCount(knowledgeChunk.tokenCount());
            dataObject.setContent(knowledgeChunk.content());
            dataObject.setContentPreview(knowledgeChunk.contentPreview());
            dataObject.setCitationLabel(knowledgeChunk.citationLabel());
            dataObject.setChunkStatus("ACTIVE");
            dataObject.setVersion(0);
            knowledgeChunkMapper.insert(dataObject);
        }
    }
}
