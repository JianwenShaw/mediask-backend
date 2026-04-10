package me.jianwen.mediask.infra.ai.client.dto;

import java.util.List;

public record PythonKnowledgePrepareResponse(List<PythonPreparedChunk> chunks) {

    public record PythonPreparedChunk(
            Integer chunkIndex,
            String content,
            String sectionTitle,
            Integer pageNo,
            Integer charStart,
            Integer charEnd,
            Integer tokenCount,
            String contentPreview,
            String citationLabel) {}
}
