package me.jianwen.mediask.api.dto;

import java.time.LocalDate;

public record AdminPatientDetailResponse(
        Long patientId,
        Long userId,
        String patientNo,
        String username,
        String displayName,
        String mobileMasked,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary,
        String accountStatus) {
}
