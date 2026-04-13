package me.jianwen.mediask.application.ai.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record GetAiSessionDetailQuery(Long patientUserId, Long sessionId) {

    public GetAiSessionDetailQuery {
        patientUserId = ArgumentChecks.requirePositive(patientUserId, "patientUserId");
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
    }
}
