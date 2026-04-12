package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public final class AiSession {

    private final Long id;
    private final String sessionUuid;
    private final Long patientId;
    private final Long departmentId;
    private final AiSceneType sceneType;
    private final AiEntrypoint entrypoint;
    private AiSessionStatus status;
    private String chiefComplaintSummary;
    private String summary;
    private final OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private Integer version;

    private AiSession(
            Long id,
            String sessionUuid,
            Long patientId,
            Long departmentId,
            AiSceneType sceneType,
            AiEntrypoint entrypoint,
            AiSessionStatus status,
            String chiefComplaintSummary,
            String summary,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer version) {
        this.id = ArgumentChecks.requirePositive(id, "id");
        this.sessionUuid = ArgumentChecks.requireNonBlank(sessionUuid, "sessionUuid");
        this.patientId = ArgumentChecks.requirePositive(patientId, "patientId");
        this.departmentId = ArgumentChecks.normalizePositive(departmentId, "departmentId");
        this.sceneType = sceneType;
        this.entrypoint = entrypoint;
        this.status = status;
        this.chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        this.summary = ArgumentChecks.blankToNull(summary);
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.version = version;
    }

    public static AiSession createActive(Long patientId, Long departmentId, AiSceneType sceneType) {
        Long id = SnowflakeIdGenerator.nextId();
        return new AiSession(
                id,
                "ai-session-" + id,
                patientId,
                departmentId,
                sceneType,
                AiEntrypoint.PATIENT_APP,
                AiSessionStatus.ACTIVE,
                null,
                null,
                OffsetDateTime.now(),
                null,
                0);
    }

    public static AiSession rehydrate(
            Long id,
            String sessionUuid,
            Long patientId,
            Long departmentId,
            AiSceneType sceneType,
            AiEntrypoint entrypoint,
            AiSessionStatus status,
            String chiefComplaintSummary,
            String summary,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            Integer version) {
        return new AiSession(
                id,
                sessionUuid,
                patientId,
                departmentId,
                sceneType,
                entrypoint,
                status,
                chiefComplaintSummary,
                summary,
                startedAt,
                endedAt,
                version);
    }

    public void updateSummary(String chiefComplaintSummary, String summary) {
        this.chiefComplaintSummary = ArgumentChecks.blankToNull(chiefComplaintSummary);
        this.summary = ArgumentChecks.blankToNull(summary);
    }

    public Long id() {
        return id;
    }

    public String sessionUuid() {
        return sessionUuid;
    }

    public Long patientId() {
        return patientId;
    }

    public Long departmentId() {
        return departmentId;
    }

    public AiSceneType sceneType() {
        return sceneType;
    }

    public AiEntrypoint entrypoint() {
        return entrypoint;
    }

    public AiSessionStatus status() {
        return status;
    }

    public String chiefComplaintSummary() {
        return chiefComplaintSummary;
    }

    public String summary() {
        return summary;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public OffsetDateTime endedAt() {
        return endedAt;
    }

    public Integer version() {
        return version;
    }
}
