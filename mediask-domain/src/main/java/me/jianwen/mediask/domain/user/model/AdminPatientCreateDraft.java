package me.jianwen.mediask.domain.user.model;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminPatientCreateDraft(
        String username,
        String passwordHash,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public AdminPatientCreateDraft {
        username = ArgumentChecks.requireNonBlank(username, "username");
        passwordHash = ArgumentChecks.requireNonBlank(passwordHash, "passwordHash");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        mobileMasked = ArgumentChecks.blankToNull(mobileMasked);
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }
}
