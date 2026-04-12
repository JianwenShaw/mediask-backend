package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public final class AiTurn {

    private final Long id;
    private final Long sessionId;
    private final Integer turnNo;
    private AiTurnStatus status;
    private final String inputHash;
    private String outputHash;
    private final OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer errorCode;
    private String errorMessage;
    private Integer version;

    private AiTurn(
            Long id,
            Long sessionId,
            Integer turnNo,
            AiTurnStatus status,
            String inputHash,
            String outputHash,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            Integer errorCode,
            String errorMessage,
            Integer version) {
        this.id = ArgumentChecks.requirePositive(id, "id");
        this.sessionId = ArgumentChecks.requirePositive(sessionId, "sessionId");
        this.turnNo = turnNo;
        this.status = status;
        this.inputHash = ArgumentChecks.blankToNull(inputHash);
        this.outputHash = ArgumentChecks.blankToNull(outputHash);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = ArgumentChecks.blankToNull(errorMessage);
        this.version = version;
    }

    public static AiTurn createProcessing(Long sessionId, Integer turnNo, String inputHash) {
        return new AiTurn(
                SnowflakeIdGenerator.nextId(),
                sessionId,
                turnNo,
                AiTurnStatus.PROCESSING,
                inputHash,
                null,
                OffsetDateTime.now(),
                null,
                null,
                null,
                0);
    }

    public static AiTurn rehydrate(
            Long id,
            Long sessionId,
            Integer turnNo,
            AiTurnStatus status,
            String inputHash,
            String outputHash,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            Integer errorCode,
            String errorMessage,
            Integer version) {
        return new AiTurn(
                id,
                sessionId,
                turnNo,
                status,
                inputHash,
                outputHash,
                startedAt,
                completedAt,
                errorCode,
                errorMessage,
                version);
    }

    public void markCompleted(String outputHash) {
        this.status = AiTurnStatus.COMPLETED;
        this.outputHash = ArgumentChecks.blankToNull(outputHash);
        this.completedAt = OffsetDateTime.now();
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markFailed(int errorCode, String errorMessage) {
        this.status = AiTurnStatus.FAILED;
        this.completedAt = OffsetDateTime.now();
        this.errorCode = errorCode;
        this.errorMessage = ArgumentChecks.blankToNull(errorMessage);
    }

    public Long id() {
        return id;
    }

    public Long sessionId() {
        return sessionId;
    }

    public Integer turnNo() {
        return turnNo;
    }

    public AiTurnStatus status() {
        return status;
    }

    public String inputHash() {
        return inputHash;
    }

    public String outputHash() {
        return outputHash;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    public Integer errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public Integer version() {
        return version;
    }
}
