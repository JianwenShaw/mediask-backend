package me.jianwen.mediask.domain.ai.model;

public record PreparedKnowledgeChunk(
        Integer chunkIndex,
        String content,
        String sectionTitle,
        Integer pageNo,
        Integer charStart,
        Integer charEnd,
        Integer tokenCount,
        String contentPreview,
        String citationLabel) {}
