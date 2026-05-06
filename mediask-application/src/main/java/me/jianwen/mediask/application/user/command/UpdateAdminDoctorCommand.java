package me.jianwen.mediask.application.user.command;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateAdminDoctorCommand(
        Long doctorId,
        String displayName,
        String phone,
        String professionalTitle,
        String introductionMasked,
        List<Long> departmentIds) {

    public UpdateAdminDoctorCommand {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }
}
