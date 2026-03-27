package me.jianwen.mediask.domain.user.model;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record PatientProfileSnapshot(
        Long patientId,
        String patientNo,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public PatientProfileSnapshot {
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        patientNo = ArgumentChecks.requireNonBlank(patientNo, "patientNo");
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        allergySummary = ArgumentChecks.blankToNull(allergySummary);
    }
}
