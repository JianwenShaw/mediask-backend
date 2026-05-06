package me.jianwen.mediask.domain.user.model;

public record AdminDepartmentDetail(
        Long id,
        Long hospitalId,
        String deptCode,
        String name,
        String deptType,
        Integer sortOrder,
        String status) {
}
