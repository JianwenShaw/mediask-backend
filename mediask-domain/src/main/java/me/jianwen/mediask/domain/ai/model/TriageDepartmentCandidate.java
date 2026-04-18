package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record TriageDepartmentCandidate(
        Long departmentId,
        String departmentName,
        String routingHint,
        List<String> aliases,
        Integer sortOrder) {

    public TriageDepartmentCandidate {
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
        departmentName = ArgumentChecks.requireNonBlank(departmentName, "departmentName");
        routingHint = ArgumentChecks.blankToNull(routingHint);
        aliases = aliases == null
                ? List.of()
                : aliases.stream()
                        .filter(alias -> alias != null && !alias.isBlank())
                        .map(String::trim)
                        .toList();
    }
}
