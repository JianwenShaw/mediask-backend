package me.jianwen.mediask.domain.user.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminDoctorUpdateDraft(
        String displayName,
        String phone,
        String professionalTitle,
        String introductionMasked,
        List<Long> departmentIds) {

    public AdminDoctorUpdateDraft {
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }
}
