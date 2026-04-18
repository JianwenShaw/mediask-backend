package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record TriageDepartmentCatalog(
        String hospitalScope, String departmentCatalogVersion, List<TriageDepartmentCandidate> departmentCandidates) {

    public TriageDepartmentCatalog {
        hospitalScope = ArgumentChecks.requireNonBlank(hospitalScope, "hospitalScope");
        departmentCatalogVersion = ArgumentChecks.requireNonBlank(departmentCatalogVersion, "departmentCatalogVersion");
        departmentCandidates = departmentCandidates == null ? List.of() : List.copyOf(departmentCandidates);
    }

    public boolean containsDepartmentId(Long departmentId) {
        return departmentCandidates.stream().anyMatch(candidate -> candidate.departmentId().equals(departmentId));
    }
}
