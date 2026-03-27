package me.jianwen.mediask.api.dto;

import java.time.LocalDate;

public record UpdatePatientProfileRequest(
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public UpdatePatientProfileRequest {
        gender = normalizeNullable(gender);
        bloodType = normalizeNullable(bloodType);
        allergySummary = normalizeNullable(allergySummary);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
