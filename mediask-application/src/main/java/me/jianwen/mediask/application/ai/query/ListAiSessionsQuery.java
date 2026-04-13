package me.jianwen.mediask.application.ai.query;

import me.jianwen.mediask.common.util.ArgumentChecks;

public record ListAiSessionsQuery(Long patientUserId) {

    public ListAiSessionsQuery {
        patientUserId = ArgumentChecks.requirePositive(patientUserId, "patientUserId");
    }
}
