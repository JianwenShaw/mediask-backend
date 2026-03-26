package me.jianwen.mediask.application.authz;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import me.jianwen.mediask.domain.user.model.DataScopeRule;

public record AuthzSubject(
        Long userId,
        Set<String> permissions,
        Set<DataScopeRule> dataScopeRules,
        Long primaryDepartmentId) {

    public AuthzSubject {
        userId = requirePositive(userId, "userId");
        permissions = normalizePermissions(permissions);
        dataScopeRules = normalizeDataScopeRules(dataScopeRules);
        primaryDepartmentId = normalizePositive(primaryDepartmentId, "primaryDepartmentId");
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissions.contains(permissionCode.trim());
    }

    private static Set<String> normalizePermissions(Set<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank()) {
                normalized.add(permission.trim());
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static Set<DataScopeRule> normalizeDataScopeRules(Set<DataScopeRule> dataScopeRules) {
        if (dataScopeRules == null || dataScopeRules.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<DataScopeRule> normalized = new LinkedHashSet<>();
        for (DataScopeRule rule : dataScopeRules) {
            if (rule != null) {
                normalized.add(rule);
            }
        }
        return Collections.unmodifiableSet(normalized);
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
}
