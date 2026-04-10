package me.jianwen.mediask.domain.ai.model;

import me.jianwen.mediask.common.id.SnowflakeIdGenerator;

public record KnowledgeChunk(
        Long id,
        Long knowledgeBaseId,
        Long documentId,
        Integer chunkIndex,
        String sectionTitle,
        Integer pageNo,
        Integer charStart,
        Integer charEnd,
        Integer tokenCount,
        String content,
        String contentPreview,
        String citationLabel) {

    public KnowledgeChunk {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (knowledgeBaseId == null) {
            throw new IllegalArgumentException("knowledgeBaseId must not be null");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("documentId must not be null");
        }
        if (chunkIndex == null) {
            throw new IllegalArgumentException("chunkIndex must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        content = content.trim();
        if (contentPreview != null && contentPreview.isBlank()) {
            contentPreview = null;
        }
        if (sectionTitle != null && sectionTitle.isBlank()) {
            sectionTitle = null;
        }
        if (citationLabel != null && citationLabel.isBlank()) {
            citationLabel = null;
        }
    }

    public static KnowledgeChunk create(
            Long knowledgeBaseId,
            Long documentId,
            Integer chunkIndex,
            String sectionTitle,
            Integer pageNo,
            Integer charStart,
            Integer charEnd,
            Integer tokenCount,
            String content,
            String contentPreview,
            String citationLabel) {
        return new KnowledgeChunk(
                SnowflakeIdGenerator.nextId(),
                knowledgeBaseId,
                documentId,
                chunkIndex,
                sectionTitle,
                pageNo,
                charStart,
                charEnd,
                tokenCount,
                content,
                contentPreview,
                citationLabel);
    }
}
