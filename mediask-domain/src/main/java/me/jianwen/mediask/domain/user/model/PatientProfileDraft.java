package me.jianwen.mediask.domain.user.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record PatientProfileDraft(
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    private static final Set<String> ALLOWED_GENDERS = Set.of("MALE", "FEMALE", "OTHER");

    public PatientProfileDraft {
        gender = normalizeGender(gender);
        bloodType = normalizeNullable(bloodType);
        allergySummary = normalizeNullable(allergySummary);
    }

    private static String normalizeGender(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        String upperCased = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_GENDERS.contains(upperCased)) {
            throw new IllegalArgumentException("gender must be one of MALE, FEMALE, OTHER");
        }
        return upperCased;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
