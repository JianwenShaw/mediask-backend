package me.jianwen.mediask.domain.user.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record AuthenticatedUser(
        Long userId,
        String username,
        String displayName,
        UserType userType,
        Set<RoleCode> roles,
        Set<String> permissions,
        Set<DataScopeRule> dataScopeRules,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId) {

    public AuthenticatedUser {
        userId = requirePositive(userId, "userId");
        username = requireNonBlank(username, "username");
        displayName = requireNonBlank(displayName, "displayName");
        userType = Objects.requireNonNull(userType, "userType must not be null");
        roles = normalizeRoles(roles);
        permissions = normalizePermissions(permissions);
        dataScopeRules = normalizeDataScopeRules(dataScopeRules);
        patientId = normalizePositive(patientId, "patientId");
        doctorId = normalizePositive(doctorId, "doctorId");
        primaryDepartmentId = normalizePositive(primaryDepartmentId, "primaryDepartmentId");
    }

    public boolean hasRole(RoleCode roleCode) {
        return roles.contains(roleCode);
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissions.contains(permissionCode.trim());
    }

    public Set<DataScopeRule> dataScopeRulesByResource(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return Set.of();
        }
        String normalizedResourceType = resourceType.trim().toUpperCase(Locale.ROOT);
        return dataScopeRules.stream()
                .filter(rule -> rule.resourceType().equals(normalizedResourceType))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean hasDataScopeType(String resourceType, DataScopeType dataScopeType) {
        if (dataScopeType == null) {
            return false;
        }
        return dataScopeRulesByResource(resourceType).stream()
                .map(DataScopeRule::scopeType)
                .anyMatch(dataScopeType::equals);
    }

    private static Set<RoleCode> normalizeRoles(Set<RoleCode> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<RoleCode> normalized = new LinkedHashSet<>();
        for (RoleCode role : roles) {
            if (role != null) {
                normalized.add(role);
            }
        }
        return Collections.unmodifiableSet(normalized);
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
        for (DataScopeRule dataScopeRule : dataScopeRules) {
            if (dataScopeRule != null) {
                normalized.add(dataScopeRule);
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

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
