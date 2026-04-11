package me.jianwen.mediask.domain.ai.port;

import me.jianwen.mediask.domain.ai.model.KnowledgeSourceType;

public interface KnowledgeDocumentStoragePort {

    String store(Long knowledgeBaseId, String originalFilename, KnowledgeSourceType sourceType, byte[] fileContent);
}
