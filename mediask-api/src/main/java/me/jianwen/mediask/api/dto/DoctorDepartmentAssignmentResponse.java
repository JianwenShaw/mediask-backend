package me.jianwen.mediask.api.dto;

public record DoctorDepartmentAssignmentResponse(
        String departmentId,
        String departmentName,
        boolean primary) {
}
