package me.jianwen.mediask.api.dto;

public record CreateKnowledgeBaseRequest(
        String name, String kbCode, String ownerType, Long ownerDeptId, String visibility) {}
