package me.jianwen.mediask.domain.user.model;

public record DoctorProfile(
        Long doctorId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        Long hospitalId,
        Long primaryDepartmentId,
        String primaryDepartmentName) {

    public DoctorProfile {
        doctorId = requirePositive(doctorId, "doctorId");
        doctorCode = requireNonBlank(doctorCode, "doctorCode");
        hospitalId = requirePositive(hospitalId, "hospitalId");
        primaryDepartmentId = normalizePositive(primaryDepartmentId, "primaryDepartmentId");
        professionalTitle = normalizeNullable(professionalTitle);
        introductionMasked = normalizeNullable(introductionMasked);
        primaryDepartmentName = normalizeNullable(primaryDepartmentName);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static Long normalizePositive(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0L) {
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
