package me.jianwen.mediask.api.dto;

import java.util.List;

public record AdminDoctorListItemResponse(
        String doctorId,
        String userId,
        String username,
        String displayName,
        String doctorCode,
        String professionalTitle,
        String primaryDepartmentName,
        String accountStatus) {
}
