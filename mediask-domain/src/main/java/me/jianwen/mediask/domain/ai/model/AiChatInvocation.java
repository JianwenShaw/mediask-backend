package me.jianwen.mediask.domain.ai.model;

import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiChatInvocation(
        Long modelRunId,
        Long turnId,
        String sessionUuid,
        String message,
        AiSceneType sceneType,
        Long departmentId,
        String hospitalScope,
        String departmentCatalogVersion,
        Integer patientTurnNoInActiveCycle,
        boolean forceFinalize,
        String contextSummary,
        boolean useRag,
        List<Long> knowledgeBaseIds) {

    public AiChatInvocation {
        modelRunId = ArgumentChecks.requirePositive(modelRunId, "modelRunId");
        turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        sessionUuid = ArgumentChecks.requireNonBlank(sessionUuid, "sessionUuid");
        message = ArgumentChecks.requireNonBlank(message, "message");
        sceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
        hospitalScope = ArgumentChecks.requireNonBlank(hospitalScope, "hospitalScope");
        departmentCatalogVersion = ArgumentChecks.requireNonBlank(departmentCatalogVersion, "departmentCatalogVersion");
        patientTurnNoInActiveCycle =
                ArgumentChecks.requirePositive(patientTurnNoInActiveCycle == null ? null : Long.valueOf(patientTurnNoInActiveCycle), "patientTurnNoInActiveCycle")
                        .intValue();
        contextSummary = ArgumentChecks.blankToNull(contextSummary);
        knowledgeBaseIds = knowledgeBaseIds == null ? null : List.copyOf(knowledgeBaseIds);
    }
}
