package me.jianwen.mediask.application.user.command;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record CreateAdminPatientCommand(
        String username,
        String phone,
        String password,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public CreateAdminPatientCommand {
        username = ArgumentChecks.requireNonBlank(username, "username");
        phone = ArgumentChecks.requireNonBlank(phone, "phone");
        password = preservePassword(password);
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        mobileMasked = ArgumentChecks.blankToNull(mobileMasked);
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }

    private static String preservePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("password must not be blank");
        }
        return value;
    }
}
