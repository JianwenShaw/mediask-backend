package me.jianwen.mediask.application.authz;

public record ResourceAccessContext(Long ownerUserId, Long departmentId) {

    public ResourceAccessContext {
        ownerUserId = normalizePositive(ownerUserId, "ownerUserId");
        departmentId = normalizePositive(departmentId, "departmentId");
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
}
