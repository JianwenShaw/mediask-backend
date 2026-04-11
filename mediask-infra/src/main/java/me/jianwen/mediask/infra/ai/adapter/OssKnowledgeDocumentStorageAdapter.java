package me.jianwen.mediask.infra.ai.adapter;

import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.infra.ai.config.KnowledgeDocumentStorageProperties;

public final class OssKnowledgeDocumentStorageAdapter implements KnowledgeDocumentStoragePort {

    private final KnowledgeDocumentStorageProperties.Oss oss;

    public OssKnowledgeDocumentStorageAdapter(KnowledgeDocumentStorageProperties.Oss oss) {
        this.oss = oss;
    }

    @Override
    public String store(Long knowledgeBaseId, String originalFilename, KnowledgeSourceType sourceType, byte[] fileContent) {
        if (oss.bucket() == null || oss.keyPrefix() == null) {
            throw new IllegalStateException("oss knowledge document storage is not configured");
        }
        // TODO: implement OSS upload and return the final object URI.
        throw new IllegalStateException("oss knowledge document storage adapter is not implemented");
    }
}
