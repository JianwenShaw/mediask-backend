package me.jianwen.mediask.api.dto;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateAdminDepartmentRequest(
        String name,
        String deptType,
        String status,
        Integer sortOrder) {

    public UpdateAdminDepartmentRequest {
        name = ArgumentChecks.requireNonBlank(name, "name");
        deptType = ArgumentChecks.requireNonBlank(deptType, "deptType");
        status = ArgumentChecks.requireNonBlank(status, "status");
        sortOrder = sortOrder == null ? 0 : sortOrder;
    }
}
