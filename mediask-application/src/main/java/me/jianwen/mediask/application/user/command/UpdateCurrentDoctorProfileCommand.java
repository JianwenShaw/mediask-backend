package me.jianwen.mediask.application.user.command;

public record UpdateCurrentDoctorProfileCommand(Long userId, String professionalTitle, String introductionMasked) {

    public UpdateCurrentDoctorProfileCommand {
        if (userId == null || userId <= 0L) {
            throw new IllegalArgumentException("userId must be greater than 0");
        }
    }
}
