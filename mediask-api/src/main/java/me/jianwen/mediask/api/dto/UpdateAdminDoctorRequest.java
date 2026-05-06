package me.jianwen.mediask.api.dto;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateAdminDoctorRequest(
        String displayName,
        String phone,
        String professionalTitle,
        String introductionMasked,
        List<Long> departmentIds) {

    public UpdateAdminDoctorRequest {
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }
}
