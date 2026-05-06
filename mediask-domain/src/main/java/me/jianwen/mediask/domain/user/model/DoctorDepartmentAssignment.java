package me.jianwen.mediask.domain.user.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record DoctorDepartmentAssignment(
        Long departmentId,
        String departmentName,
        boolean primary) {

    public DoctorDepartmentAssignment {
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
        departmentName = ArgumentChecks.requireNonBlank(departmentName, "departmentName");
    }
}
