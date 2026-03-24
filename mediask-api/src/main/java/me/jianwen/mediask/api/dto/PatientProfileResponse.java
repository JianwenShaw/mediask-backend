package me.jianwen.mediask.api.dto;

import java.time.LocalDate;

public record PatientProfileResponse(
        Long patientId,
        String patientNo,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {
}
