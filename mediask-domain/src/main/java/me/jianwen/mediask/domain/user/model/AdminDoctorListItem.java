package me.jianwen.mediask.domain.user.model;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminDoctorListItem(
        Long doctorId,
        Long userId,
        String username,
        String displayName,
        String doctorCode,
        String professionalTitle,
        String primaryDepartmentName,
        String accountStatus) {

    public AdminDoctorListItem {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
        userId = ArgumentChecks.requirePositive(userId, "userId");
        username = ArgumentChecks.requireNonBlank(username, "username");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        doctorCode = ArgumentChecks.requireNonBlank(doctorCode, "doctorCode");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        primaryDepartmentName = ArgumentChecks.blankToNull(primaryDepartmentName);
        accountStatus = ArgumentChecks.requireNonBlank(accountStatus, "accountStatus");
    }
}
