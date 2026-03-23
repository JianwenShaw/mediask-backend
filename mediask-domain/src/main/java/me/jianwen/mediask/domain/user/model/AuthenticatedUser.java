package me.jianwen.mediask.domain.user.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String username,
        String displayName,
        UserType userType,
        Set<RoleCode> roles,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId) {

    public AuthenticatedUser {
        userId = requirePositive(userId, "userId");
        username = requireNonBlank(username, "username");
        displayName = requireNonBlank(displayName, "displayName");
        userType = Objects.requireNonNull(userType, "userType must not be null");
        roles = normalizeRoles(roles);
        patientId = normalizePositive(patientId, "patientId");
        doctorId = normalizePositive(doctorId, "doctorId");
        primaryDepartmentId = normalizePositive(primaryDepartmentId, "primaryDepartmentId");
    }

    public boolean hasRole(RoleCode roleCode) {
        return roles.contains(roleCode);
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
