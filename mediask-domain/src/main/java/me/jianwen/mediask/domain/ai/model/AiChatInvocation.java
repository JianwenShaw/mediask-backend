package me.jianwen.mediask.domain.ai.model;

import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiChatInvocation(
        Long modelRunId,
        Long turnId,
        String sessionUuid,
        String message,
        AiSceneType sceneType,
        Long departmentId,
        String contextSummary,
        boolean useRag) {

    public AiChatInvocation {
        modelRunId = ArgumentChecks.requirePositive(modelRunId, "modelRunId");
        turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        sessionUuid = ArgumentChecks.requireNonBlank(sessionUuid, "sessionUuid");
        message = ArgumentChecks.requireNonBlank(message, "message");
        sceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
        contextSummary = ArgumentChecks.blankToNull(contextSummary);
    }
}
