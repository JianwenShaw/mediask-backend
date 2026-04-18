package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiModelRun;
import me.jianwen.mediask.domain.ai.model.AiTriageSnapshot;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiModelRunDO;
import me.jianwen.mediask.infra.persistence.mapper.AiModelRunMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class AiModelRunRepositoryAdapter implements AiModelRunRepository {

    private final AiModelRunMapper aiModelRunMapper;
    private final ObjectMapper objectMapper;

    public AiModelRunRepositoryAdapter(
            AiModelRunMapper aiModelRunMapper, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.aiModelRunMapper = aiModelRunMapper;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new).copy().findAndRegisterModules();
    }

    @Override
    public void save(AiModelRun aiModelRun) {
        aiModelRunMapper.insert(toDataObject(aiModelRun));
    }

    @Override
    public void update(AiModelRun aiModelRun) {
        AiModelRunDO existing = aiModelRunMapper.selectOne(Wrappers.lambdaQuery(AiModelRunDO.class)
                .eq(AiModelRunDO::getId, aiModelRun.id())
                .isNull(AiModelRunDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.AI_MODEL_RUN_UPDATE_CONFLICT);
        }

        AiModelRunDO toUpdate = new AiModelRunDO();
        toUpdate.setId(existing.getId());
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setProviderRunId(aiModelRun.providerRunId());
        toUpdate.setModelName(aiModelRun.modelName());
        toUpdate.setRetrievalProvider(aiModelRun.retrievalProvider());
        toUpdate.setRunStatus(aiModelRun.status().name());
        toUpdate.setIsDegraded(aiModelRun.degraded());
        toUpdate.setTokensInput(aiModelRun.tokensInput());
        toUpdate.setTokensOutput(aiModelRun.tokensOutput());
        toUpdate.setLatencyMs(aiModelRun.latencyMs());
        toUpdate.setResponsePayloadHash(aiModelRun.responsePayloadHash());
        toUpdate.setErrorCode(aiModelRun.errorCode());
        toUpdate.setErrorMessage(aiModelRun.errorMessage());
        toUpdate.setTriageSnapshotJson(writeJson(aiModelRun.triageSnapshot()));
        toUpdate.setCompletedAt(aiModelRun.completedAt());
        if (aiModelRunMapper.updateById(toUpdate) != 1) {
            throw new BizException(AiErrorCode.AI_MODEL_RUN_UPDATE_CONFLICT);
        }
    }

    @Override
    public Integer findLatestFinalizedTurnNoBySessionId(Long sessionId) {
        return aiModelRunMapper.selectLatestFinalizedTurnNoBySessionId(sessionId);
    }

    private AiModelRunDO toDataObject(AiModelRun aiModelRun) {
        AiModelRunDO dataObject = new AiModelRunDO();
        dataObject.setId(aiModelRun.id());
        dataObject.setTurnId(aiModelRun.turnId());
        dataObject.setProviderName(aiModelRun.providerName());
        dataObject.setProviderRunId(aiModelRun.providerRunId());
        dataObject.setModelName(aiModelRun.modelName());
        dataObject.setRequestId(aiModelRun.requestId());
        dataObject.setRagEnabled(aiModelRun.ragEnabled());
        dataObject.setRetrievalProvider(aiModelRun.retrievalProvider());
        dataObject.setRunStatus(aiModelRun.status().name());
        dataObject.setIsDegraded(aiModelRun.degraded());
        dataObject.setTokensInput(aiModelRun.tokensInput());
        dataObject.setTokensOutput(aiModelRun.tokensOutput());
        dataObject.setLatencyMs(aiModelRun.latencyMs());
        dataObject.setRequestPayloadHash(aiModelRun.requestPayloadHash());
        dataObject.setResponsePayloadHash(aiModelRun.responsePayloadHash());
        dataObject.setErrorCode(aiModelRun.errorCode());
        dataObject.setErrorMessage(aiModelRun.errorMessage());
        dataObject.setTriageSnapshotJson(writeJson(aiModelRun.triageSnapshot()));
        dataObject.setStartedAt(aiModelRun.startedAt());
        dataObject.setCompletedAt(aiModelRun.completedAt());
        dataObject.setVersion(aiModelRun.version());
        return dataObject;
    }

    private String writeJson(AiTriageSnapshot triageSnapshot) {
        if (triageSnapshot == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(triageSnapshot);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize ai triage snapshot", exception);
        }
    }
}
