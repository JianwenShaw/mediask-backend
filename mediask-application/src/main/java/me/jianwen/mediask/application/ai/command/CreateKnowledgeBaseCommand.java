package me.jianwen.mediask.application.ai.command;

public record CreateKnowledgeBaseCommand(
        String name, String kbCode, String ownerType, Long ownerDeptId, String visibility) {}
