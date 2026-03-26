package me.jianwen.mediask.api.dto;

public record DataScopeRuleResponse(
        String resourceType,
        String scopeType,
        Long scopeDepartmentId) {
}
