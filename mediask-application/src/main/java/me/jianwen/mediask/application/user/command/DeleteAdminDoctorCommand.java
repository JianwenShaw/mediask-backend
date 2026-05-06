package me.jianwen.mediask.application.user.command;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record DeleteAdminDoctorCommand(Long doctorId) {

    public DeleteAdminDoctorCommand {
        doctorId = ArgumentChecks.requirePositive(doctorId, "doctorId");
    }
}
