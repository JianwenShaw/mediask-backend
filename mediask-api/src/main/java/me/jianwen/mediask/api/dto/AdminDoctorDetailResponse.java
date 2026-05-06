package me.jianwen.mediask.api.dto;

import java.util.List;

public record AdminDoctorDetailResponse(
        String doctorId,
        String userId,
        String username,
        String displayName,
        String phone,
        String hospitalId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        List<DoctorDepartmentAssignmentResponse> departments,
        String accountStatus) {
}
