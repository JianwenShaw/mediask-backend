package me.jianwen.mediask.api.dto;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record UpdatePatientProfileRequest(
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public UpdatePatientProfileRequest {
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }
}
