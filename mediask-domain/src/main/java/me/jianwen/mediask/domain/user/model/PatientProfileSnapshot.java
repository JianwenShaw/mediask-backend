package me.jianwen.mediask.domain.user.model;

import java.time.LocalDate;

public record PatientProfileSnapshot(
        Long patientId,
        String patientNo,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public PatientProfileSnapshot {
        patientId = requirePositive(patientId, "patientId");
        patientNo = requireNonBlank(patientNo, "patientNo");
        gender = normalizeNullable(gender);
        bloodType = normalizeNullable(bloodType);
        allergySummary = normalizeNullable(allergySummary);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
