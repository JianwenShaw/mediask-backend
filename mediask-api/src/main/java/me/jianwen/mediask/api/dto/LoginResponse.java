package me.jianwen.mediask.api.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        Long userId,
        String username,
        String displayName,
        String userType,
        List<String> roles,
        Long patientId,
        Long doctorId,
        Long primaryDepartmentId) {
}
