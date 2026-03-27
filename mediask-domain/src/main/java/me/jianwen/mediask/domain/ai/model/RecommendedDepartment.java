package me.jianwen.mediask.domain.ai.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record RecommendedDepartment(Long departmentId, String departmentName, Integer priority, String reason) {

    public RecommendedDepartment {
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
        departmentName = ArgumentChecks.requireNonBlank(departmentName, "departmentName");
        priority = ArgumentChecks.normalizePositive(priority, "priority");
        reason = ArgumentChecks.blankToNull(reason);
    }
}
