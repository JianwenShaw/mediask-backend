package me.jianwen.mediask.api.dto;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record CreateAdminDepartmentRequest(
        Long hospitalId,
        String name,
        String deptType) {

    public CreateAdminDepartmentRequest {
        hospitalId = ArgumentChecks.requirePositive(hospitalId, "hospitalId");
        name = ArgumentChecks.requireNonBlank(name, "name");
        deptType = ArgumentChecks.requireNonBlank(deptType, "deptType");
    }
}
