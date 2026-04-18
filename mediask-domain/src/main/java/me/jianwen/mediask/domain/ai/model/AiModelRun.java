package me.jianwen.mediask.domain.ai.model;

import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.common.id.SnowflakeIdGenerator;
import me.jianwen.mediask.common.util.ArgumentChecks;

public final class AiModelRun {

    private final Long id;
    private final Long turnId;
    private final String providerName;
    private String providerRunId;
    private String modelName;
    private final String requestId;
    private final boolean ragEnabled;
    private String retrievalProvider;
    private AiModelRunStatus status;
    private boolean degraded;
    private Integer tokensInput;
    private Integer tokensOutput;
    private Integer latencyMs;
    private final String requestPayloadHash;
    private String responsePayloadHash;
    private Integer errorCode;
    private String errorMessage;
    private final OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Integer version;
    private List<String> matchedRuleCodes;
    private AiTriageSnapshot triageSnapshot;

    private AiModelRun(
            Long id,
            Long turnId,
            String providerName,
            String providerRunId,
            String modelName,
            String requestId,
            boolean ragEnabled,
            String retrievalProvider,
            AiModelRunStatus status,
            boolean degraded,
            Integer tokensInput,
            Integer tokensOutput,
            Integer latencyMs,
            String requestPayloadHash,
            String responsePayloadHash,
            Integer errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            Integer version,
            List<String> matchedRuleCodes,
            AiTriageSnapshot triageSnapshot) {
        this.id = ArgumentChecks.requirePositive(id, "id");
        this.turnId = ArgumentChecks.requirePositive(turnId, "turnId");
        this.providerName = ArgumentChecks.requireNonBlank(providerName, "providerName");
        this.providerRunId = ArgumentChecks.blankToNull(providerRunId);
        this.modelName = ArgumentChecks.blankToNull(modelName);
        this.requestId = ArgumentChecks.requireNonBlank(requestId, "requestId");
        this.ragEnabled = ragEnabled;
        this.retrievalProvider = ArgumentChecks.blankToNull(retrievalProvider);
        this.status = status;
        this.degraded = degraded;
        this.tokensInput = tokensInput;
        this.tokensOutput = tokensOutput;
        this.latencyMs = latencyMs;
        this.requestPayloadHash = ArgumentChecks.blankToNull(requestPayloadHash);
        this.responsePayloadHash = ArgumentChecks.blankToNull(responsePayloadHash);
        this.errorCode = errorCode;
        this.errorMessage = ArgumentChecks.blankToNull(errorMessage);
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.version = version;
        this.matchedRuleCodes = matchedRuleCodes == null ? List.of() : List.copyOf(matchedRuleCodes);
        this.triageSnapshot = triageSnapshot;
    }

    public static AiModelRun createRunning(Long turnId, String requestId, String requestPayloadHash, boolean ragEnabled) {
        return new AiModelRun(
                SnowflakeIdGenerator.nextId(),
                turnId,
                "PYTHON_AI",
                null,
                null,
                requestId,
                ragEnabled,
                null,
                AiModelRunStatus.RUNNING,
                false,
                null,
                null,
                null,
                requestPayloadHash,
                null,
                null,
                null,
                OffsetDateTime.now(),
                null,
                0,
                List.of(),
                null);
    }

    public static AiModelRun rehydrate(
            Long id,
            Long turnId,
            String providerName,
            String providerRunId,
            String modelName,
            String requestId,
            boolean ragEnabled,
            String retrievalProvider,
            AiModelRunStatus status,
            boolean degraded,
            Integer tokensInput,
            Integer tokensOutput,
            Integer latencyMs,
            String requestPayloadHash,
            String responsePayloadHash,
            Integer errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            Integer version,
            List<String> matchedRuleCodes,
            AiTriageSnapshot triageSnapshot) {
        return new AiModelRun(
                id,
                turnId,
                providerName,
                providerRunId,
                modelName,
                requestId,
                ragEnabled,
                retrievalProvider,
                status,
                degraded,
                tokensInput,
                tokensOutput,
                latencyMs,
                requestPayloadHash,
                responsePayloadHash,
                errorCode,
                errorMessage,
                startedAt,
                completedAt,
                version,
                matchedRuleCodes,
                triageSnapshot);
    }

    public void markSucceeded(AiExecutionMetadata metadata, String responsePayloadHash) {
        markSucceeded(metadata, responsePayloadHash, null);
    }

    public void markSucceeded(
            AiExecutionMetadata metadata, String responsePayloadHash, AiTriageSnapshot triageSnapshot) {
        this.providerRunId = metadata.providerRunId();
        this.status = AiModelRunStatus.SUCCEEDED;
        this.degraded = metadata.degraded();
        this.tokensInput = metadata.tokensInput();
        this.tokensOutput = metadata.tokensOutput();
        this.latencyMs = metadata.latencyMs();
        this.responsePayloadHash = ArgumentChecks.blankToNull(responsePayloadHash);
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = OffsetDateTime.now();
        this.matchedRuleCodes = metadata.matchedRuleCodes() == null ? List.of() : List.copyOf(metadata.matchedRuleCodes());
        this.triageSnapshot = triageSnapshot;
    }

    public void markFailed(int errorCode, String errorMessage) {
        this.status = AiModelRunStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = ArgumentChecks.blankToNull(errorMessage);
        this.completedAt = OffsetDateTime.now();
    }

    public Long id() {
        return id;
    }

    public Long turnId() {
        return turnId;
    }

    public String providerName() {
        return providerName;
    }

    public String providerRunId() {
        return providerRunId;
    }

    public String modelName() {
        return modelName;
    }

    public String requestId() {
        return requestId;
    }

    public boolean ragEnabled() {
        return ragEnabled;
    }

    public String retrievalProvider() {
        return retrievalProvider;
    }

    public AiModelRunStatus status() {
        return status;
    }

    public boolean degraded() {
        return degraded;
    }

    public Integer tokensInput() {
        return tokensInput;
    }

    public Integer tokensOutput() {
        return tokensOutput;
    }

    public Integer latencyMs() {
        return latencyMs;
    }

    public String requestPayloadHash() {
        return requestPayloadHash;
    }

    public String responsePayloadHash() {
        return responsePayloadHash;
    }

    public Integer errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public OffsetDateTime startedAt() {
        return startedAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    public Integer version() {
        return version;
    }

    public List<String> matchedRuleCodes() {
        return matchedRuleCodes;
    }

    public AiTriageSnapshot triageSnapshot() {
        return triageSnapshot;
    }
}
