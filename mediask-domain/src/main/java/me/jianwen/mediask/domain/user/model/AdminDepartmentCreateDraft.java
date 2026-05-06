package me.jianwen.mediask.domain.user.model;

public record AdminDepartmentCreateDraft(
        Long hospitalId,
        String name,
        String deptType) {
}
