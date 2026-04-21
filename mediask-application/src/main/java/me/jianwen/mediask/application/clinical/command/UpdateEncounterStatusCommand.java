package me.jianwen.mediask.application.clinical.command;

public record UpdateEncounterStatusCommand(Long encounterId, Long doctorId, Action action) {

    public enum Action {
        START,
        COMPLETE
    }
}
