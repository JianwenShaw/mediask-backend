package me.jianwen.mediask.domain.user.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminDoctorDetail(
        Long doctorId,
        Long userId,
        String username,
        String displayName,
        String phone,
        Long hospitalId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        List<DoctorDepartmentAssignment> departments,
        String accountStatus) {

    public AdminDoctorDetail {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
        userId = ArgumentChecks.requirePositive(userId, "userId");
        username = ArgumentChecks.requireNonBlank(username, "username");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        hospitalId = ArgumentChecks.requirePositive(hospitalId, "hospitalId");
        doctorCode = ArgumentChecks.requireNonBlank(doctorCode, "doctorCode");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departments = departments == null ? List.of() : List.copyOf(departments);
        accountStatus = ArgumentChecks.requireNonBlank(accountStatus, "accountStatus");
    }
}
