package me.jianwen.mediask.api.dto;

import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        List<String> permissions,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId) {
}
