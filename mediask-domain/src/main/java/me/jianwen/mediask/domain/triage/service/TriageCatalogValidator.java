package me.jianwen.mediask.domain.triage.service;

import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.triage.exception.TriageErrorCode;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;

public final class TriageCatalogValidator {

    private TriageCatalogValidator() {}

    public static void validate(TriageCatalog catalog, Long departmentId, String departmentName) {
        DepartmentCandidate candidate = catalog.findCandidate(departmentId)
                .orElseThrow(() -> new BizException(TriageErrorCode.DEPARTMENT_NOT_IN_CATALOG));
        if (!candidate.departmentName().equals(departmentName)) {
            throw new BizException(TriageErrorCode.DEPARTMENT_NAME_MISMATCH);
        }
    }
}
