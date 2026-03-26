package me.jianwen.mediask.domain.user.model;

import java.util.Locale;
import java.util.Objects;

public record DataScopeRule(String resourceType, DataScopeType scopeType, Long scopeDepartmentId) {

    public DataScopeRule {
        resourceType = normalizeResourceType(resourceType);
        scopeType = Objects.requireNonNull(scopeType, "scopeType must not be null");
        scopeDepartmentId = normalizePositive(scopeDepartmentId, "scopeDepartmentId");
    }

    public boolean matchesResourceType(String resourceType) {
        return this.resourceType.equals(normalizeResourceType(resourceType));
    }

    private static String normalizeResourceType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
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
