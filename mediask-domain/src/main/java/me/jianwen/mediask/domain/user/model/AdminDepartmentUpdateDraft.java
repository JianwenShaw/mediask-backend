package me.jianwen.mediask.domain.user.model;

public record AdminDepartmentUpdateDraft(
        String name,
        String deptType,
        String status,
        Integer sortOrder) {
}
