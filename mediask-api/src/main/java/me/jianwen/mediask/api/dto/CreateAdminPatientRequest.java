package me.jianwen.mediask.api.dto;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record CreateAdminPatientRequest(
        String username,
        String password,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public CreateAdminPatientRequest {
        username = ArgumentChecks.requireNonBlank(username, "username");
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
