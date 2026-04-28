package me.jianwen.mediask.domain.triage.port;

import java.util.List;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;

public interface DepartmentCatalogSourcePort {

    List<DepartmentCandidate> loadCandidates(String hospitalScope);
}
