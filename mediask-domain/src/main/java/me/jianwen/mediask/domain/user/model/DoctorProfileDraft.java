package me.jianwen.mediask.domain.user.model;

public record DoctorProfileDraft(String professionalTitle, String introductionMasked) {

    public DoctorProfileDraft {
        professionalTitle = normalizeNullable(professionalTitle);
        introductionMasked = normalizeNullable(introductionMasked);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
