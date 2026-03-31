package me.jianwen.mediask.domain.user.model;

import java.time.LocalDate;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AdminPatientListItem(
        Long patientId,
        Long userId,
        String patientNo,
        String username,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String accountStatus) {

    public AdminPatientListItem {
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        userId = ArgumentChecks.requirePositive(userId, "userId");
        patientNo = ArgumentChecks.requireNonBlank(patientNo, "patientNo");
        username = ArgumentChecks.requireNonBlank(username, "username");
        displayName = ArgumentChecks.requireNonBlank(displayName, "displayName");
        mobileMasked = ArgumentChecks.blankToNull(mobileMasked);
        gender = ArgumentChecks.blankToNull(gender);
        bloodType = ArgumentChecks.blankToNull(bloodType);
        accountStatus = ArgumentChecks.requireNonBlank(accountStatus, "accountStatus");
    }
}
