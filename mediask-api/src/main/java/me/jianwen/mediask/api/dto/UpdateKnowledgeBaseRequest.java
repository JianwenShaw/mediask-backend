package me.jianwen.mediask.api.dto;

public record UpdateKnowledgeBaseRequest(
        String name, String ownerType, Long ownerDeptId, String visibility, String status) {}
