package me.jianwen.mediask.domain.ai.model;

public record AiTriageRecommendedDepartment(
        Long departmentId,
        String departmentName,
        Integer priority,
        String reason) {}
