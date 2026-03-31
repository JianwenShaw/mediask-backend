package me.jianwen.mediask.application.user.command;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record DeleteAdminPatientCommand(Long patientId) {

    public DeleteAdminPatientCommand {
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
    }
}
