package me.jianwen.mediask.api.dto;

public record UpdateDoctorProfileRequest(String professionalTitle, String introductionMasked) {

    public UpdateDoctorProfileRequest {
        professionalTitle = normalizeNullable(professionalTitle);
        introductionMasked = normalizeNullable(introductionMasked);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
