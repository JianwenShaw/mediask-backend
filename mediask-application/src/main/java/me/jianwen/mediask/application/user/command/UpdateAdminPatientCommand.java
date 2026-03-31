package me.jianwen.mediask.application.user.command;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdateAdminPatientCommand(
        Long patientId,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public UpdateAdminPatientCommand {
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        mobileMasked = ArgumentChecks.blankToNull(mobileMasked);
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }
}
