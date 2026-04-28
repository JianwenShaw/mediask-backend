package me.jianwen.mediask.domain.triage.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record TriageCatalog(
        String hospitalScope,
        CatalogVersion catalogVersion,
        OffsetDateTime publishedAt,
        List<DepartmentCandidate> departmentCandidates) {

    public TriageCatalog {
        hospitalScope = ArgumentChecks.requireNonBlank(hospitalScope, "hospitalScope");
        ArgumentChecks.requireNonNull(catalogVersion, "catalogVersion");
        ArgumentChecks.requireNonNull(publishedAt, "publishedAt");
        departmentCandidates = List.copyOf(ArgumentChecks.requireNonNull(departmentCandidates, "departmentCandidates"));
        if (departmentCandidates.isEmpty()) {
            throw new IllegalArgumentException("departmentCandidates must not be empty");
        }
    }

    public Optional<DepartmentCandidate> findCandidate(Long departmentId) {
        return departmentCandidates.stream()
                .filter(c -> c.departmentId().equals(departmentId))
                .findFirst();
    }

    public boolean containsDepartment(Long departmentId) {
        return findCandidate(departmentId).isPresent();
    }

    public int candidateCount() {
        return departmentCandidates.size();
    }
}
