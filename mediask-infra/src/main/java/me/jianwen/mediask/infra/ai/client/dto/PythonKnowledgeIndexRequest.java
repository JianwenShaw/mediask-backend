package me.jianwen.mediask.infra.ai.client.dto;

import java.util.List;

public record PythonKnowledgeIndexRequest(Long documentId, Long knowledgeBaseId, List<PythonKnowledgeChunk> chunks) {

    public record PythonKnowledgeChunk(Long chunkId, Integer chunkIndex, String content) {}
}
