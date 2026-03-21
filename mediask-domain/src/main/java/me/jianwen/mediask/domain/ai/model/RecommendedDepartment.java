package me.jianwen.mediask.domain.ai.model;

public record RecommendedDepartment(Long departmentId, String departmentName, Integer priority, String reason) {

    public RecommendedDepartment {
        departmentId = requirePositive(departmentId, "departmentId");
        departmentName = requireNonBlank(departmentName, "departmentName");
        priority = normalizePositive(priority, "priority");
        reason = normalizeBlank(reason);
    }

    private static Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0");
        }
        return value;
    }

    private static Integer normalizePositive(Integer value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
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

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
