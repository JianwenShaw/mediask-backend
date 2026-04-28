package me.jianwen.mediask.domain.triage.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record DepartmentCandidate(
        Long departmentId,
        String departmentName,
        String routingHint,
        List<String> aliases,
        int sortOrder) {

    public DepartmentCandidate {
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
        departmentName = ArgumentChecks.requireNonBlank(departmentName, "departmentName");
        routingHint = ArgumentChecks.requireNonBlank(routingHint, "routingHint");
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        if (sortOrder < 0) {
            throw new IllegalArgumentException("sortOrder must be >= 0");
        }
    }
}
