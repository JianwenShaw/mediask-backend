package me.jianwen.mediask.api.dto;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateAdminPatientRequest(
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public UpdateAdminPatientRequest {
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        mobileMasked = ArgumentChecks.blankToNull(mobileMasked);
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }
}
