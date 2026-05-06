package me.jianwen.mediask.application.user.command;

public record UpdateAdminDepartmentCommand(
        Long id,
        String name,
        String deptType,
        String status,
        Integer sortOrder) {
}
