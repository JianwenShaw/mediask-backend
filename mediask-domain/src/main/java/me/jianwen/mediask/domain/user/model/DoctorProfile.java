package me.jianwen.mediask.domain.user.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record DoctorProfile(
        Long doctorId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        Long hospitalId,
        Long primaryDepartmentId,
        String primaryDepartmentName) {

    public DoctorProfile {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
        doctorCode = ArgumentChecks.requireNonBlank(doctorCode, "doctorCode");
        hospitalId = ArgumentChecks.requirePositive(hospitalId, "hospitalId");
        primaryDepartmentId = ArgumentChecks.normalizePositive(primaryDepartmentId, "primaryDepartmentId");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        primaryDepartmentName = ArgumentChecks.blankToNull(primaryDepartmentName);
    }
}
