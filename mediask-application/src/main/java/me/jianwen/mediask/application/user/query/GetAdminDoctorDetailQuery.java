package me.jianwen.mediask.application.user.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record GetAdminDoctorDetailQuery(Long doctorId) {

    public GetAdminDoctorDetailQuery {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
    }
}
