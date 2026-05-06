package me.jianwen.mediask.application.user.command;

public record CreateAdminDepartmentCommand(
        Long hospitalId,
        String name,
        String deptType) {
}
