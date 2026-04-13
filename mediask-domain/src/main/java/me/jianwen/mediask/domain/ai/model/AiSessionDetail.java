package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import me.jianwen.mediask.common.util.ArgumentChecks;

public record AiSessionDetail(
        Long sessionId,
        Long patientId,
        Long departmentId,
        AiSceneType sceneType,
        AiSessionStatus status,
        String chiefComplaintSummary,
        String summary,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        List<AiSessionTurnDetail> turns) {

    public AiSessionDetail {
        sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
        sceneType = Objects.requireNonNull(sceneType, "sceneType must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        summary = ArgumentChecks.blankToNull(summary);
        startedAt = Objects.requireNonNull(startedAt, "startedAt must not be null");
        turns = turns == null ? List.of() : List.copyOf(turns);
    }
}
