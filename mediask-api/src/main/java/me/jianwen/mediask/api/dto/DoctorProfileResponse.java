package me.jianwen.mediask.api.dto;

public record DoctorProfileResponse(
        Long doctorId,
        String doctorCode,
        String professionalTitle,
        String introductionMasked,
        Long hospitalId,
        Long primaryDepartmentId,
        String primaryDepartmentName) {
}
