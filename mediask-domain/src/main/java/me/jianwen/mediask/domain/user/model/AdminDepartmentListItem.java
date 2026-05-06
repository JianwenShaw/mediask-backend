package me.jianwen.mediask.domain.user.model;

public record AdminDepartmentListItem(
        Long id,
        Long hospitalId,
        String deptCode,
        String name,
        String deptType,
        Integer sortOrder,
        String status) {
}
