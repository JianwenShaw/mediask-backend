package me.jianwen.mediask.application.user.command;

import java.time.LocalDate;

public record UpdateCurrentPatientProfileCommand(
        Long userId,
        String gender,
        LocalDate birthDate,
        String bloodType,
        String allergySummary) {

    public UpdateCurrentPatientProfileCommand {
        if (userId == null || userId <= 0L) {
            throw new IllegalArgumentException("userId must be greater than 0");
        }
    }
}
