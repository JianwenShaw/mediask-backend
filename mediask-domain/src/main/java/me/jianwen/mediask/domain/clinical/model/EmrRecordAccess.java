package me.jianwen.mediask.domain.clinical.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record EmrRecordAccess(Long recordId, Long patientId, Long departmentId) {

    public EmrRecordAccess {
        recordId = ArgumentChecks.requirePositive(recordId, "recordId");
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        departmentId = ArgumentChecks.requirePositive(departmentId, "departmentId");
    }
}
