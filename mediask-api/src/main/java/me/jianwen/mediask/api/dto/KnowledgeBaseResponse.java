package me.jianwen.mediask.api.dto;

public record KnowledgeBaseResponse(
        Long id,
        String kbCode,
        String name,
        String ownerType,
        Long ownerDeptId,
        String visibility,
        String status,
        Long docCount) {}
