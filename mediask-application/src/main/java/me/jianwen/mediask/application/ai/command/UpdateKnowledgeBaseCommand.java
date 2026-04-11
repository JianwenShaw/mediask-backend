package me.jianwen.mediask.application.ai.command;

public record UpdateKnowledgeBaseCommand(
        Long knowledgeBaseId, String name, String ownerType, Long ownerDeptId, String visibility, String status) {}
