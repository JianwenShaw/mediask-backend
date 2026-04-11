package me.jianwen.mediask.domain.ai.model;

public record KnowledgeBaseSummary(
        Long id,
        String kbCode,
        String name,
        KnowledgeBaseOwnerType ownerType,
        Long ownerDeptId,
        KnowledgeBaseVisibility visibility,
        KnowledgeBaseStatus status,
        long docCount) {}
