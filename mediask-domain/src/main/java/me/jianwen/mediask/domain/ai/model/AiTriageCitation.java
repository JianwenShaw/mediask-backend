package me.jianwen.mediask.domain.ai.model;

public record AiTriageCitation(
        Integer citationOrder,
        String chunkId,
        String snippet) {}
