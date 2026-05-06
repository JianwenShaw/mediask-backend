package me.jianwen.mediask.domain.user.model;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminDoctorCreateDraft(
        String username,
        String phone,
        String passwordHash,
        String displayName,
        Long hospitalId,
        String professionalTitle,
        String introductionMasked,
        List<Long> departmentIds) {

    public AdminDoctorCreateDraft {
        username = ArgumentChecks.requireNonBlank(username, "username");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        passwordHash = ArgumentChecks.requireNonBlank(passwordHash, "passwordHash");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        hospitalId = ArgumentChecks.requirePositive(hospitalId, "hospitalId");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }
}
