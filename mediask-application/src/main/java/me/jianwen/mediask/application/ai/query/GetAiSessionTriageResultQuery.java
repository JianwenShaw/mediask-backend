package me.jianwen.mediask.application.ai.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record GetAiSessionTriageResultQuery(Long patientUserId, Long sessionId) {

    public GetAiSessionTriageResultQuery {
        patientUserId = ArgumentChecks.requirePositive(patientUserId, "patientUserId");
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
    }
}
