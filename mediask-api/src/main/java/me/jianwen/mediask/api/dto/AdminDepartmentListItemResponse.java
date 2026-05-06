package me.jianwen.mediask.api.dto;

public record AdminDepartmentListItemResponse(
        String id,
        String hospitalId,
        String deptCode,
        String name,
        String deptType,
        Integer sortOrder,
        String status) {
}
