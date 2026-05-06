package me.jianwen.mediask.application.user.command;

import java.util.List;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record CreateAdminDoctorCommand(
        String username,
        String phone,
        String password,
        String displayName,
        Long hospitalId,
        String professionalTitle,
        String introductionMasked,
        List<Long> departmentIds) {

    public CreateAdminDoctorCommand {
        username = ArgumentChecks.requireNonBlank(username, "username");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        password = preservePassword(password);
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        hospitalId = ArgumentChecks.requirePositive(hospitalId, "hospitalId");
        professionalTitle = ArgumentChecks.blankToNull(professionalTitle);
        introductionMasked = ArgumentChecks.blankToNull(introductionMasked);
        departmentIds = departmentIds == null ? List.of() : List.copyOf(departmentIds);
    }

    private static String preservePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return value;
    }
}
