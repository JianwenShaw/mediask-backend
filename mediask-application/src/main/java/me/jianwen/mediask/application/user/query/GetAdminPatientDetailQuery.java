package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record GetAdminPatientDetailQuery(Long patientId) {

    public GetAdminPatientDetailQuery {
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
    }
}
