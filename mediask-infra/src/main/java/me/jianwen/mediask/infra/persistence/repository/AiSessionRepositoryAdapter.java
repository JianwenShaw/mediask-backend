package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiEntrypoint;
import me.jianwen.mediask.domain.ai.model.AiSession;
import me.jianwen.mediask.domain.ai.model.AiSessionStatus;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiSessionDO;
import me.jianwen.mediask.infra.persistence.mapper.AiSessionMapper;
import org.springframework.stereotype.Component;

@Component
public class AiSessionRepositoryAdapter implements AiSessionRepository {

    private final AiSessionMapper aiSessionMapper;

    public AiSessionRepositoryAdapter(AiSessionMapper aiSessionMapper) {
        this.aiSessionMapper = aiSessionMapper;
    }

    @Override
    public void save(AiSession aiSession) {
        aiSessionMapper.insert(toDataObject(aiSession));
    }

    @Override
    public Optional<AiSession> findById(Long sessionId) {
        AiSessionDO dataObject = aiSessionMapper.selectOne(Wrappers.lambdaQuery(AiSessionDO.class)
                .eq(AiSessionDO::getId, sessionId)
                .isNull(AiSessionDO::getDeletedAt));
        return Optional.ofNullable(dataObject).map(this::toDomain);
    }

    @Override
    public void update(AiSession aiSession) {
        AiSessionDO existing = aiSessionMapper.selectOne(Wrappers.lambdaQuery(AiSessionDO.class)
                .eq(AiSessionDO::getId, aiSession.id())
                .isNull(AiSessionDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.AI_SESSION_NOT_FOUND);
        }

        AiSessionDO toUpdate = new AiSessionDO();
        toUpdate.setId(existing.getId());
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setChiefComplaintSummary(aiSession.chiefComplaintSummary());
        toUpdate.setSummary(aiSession.summary());
        toUpdate.setSessionStatus(aiSession.status().name());
        toUpdate.setEndedAt(aiSession.endedAt());
        if (aiSessionMapper.updateById(toUpdate) != 1) {
            throw new BizException(AiErrorCode.AI_SESSION_UPDATE_CONFLICT);
        }
    }

    private AiSessionDO toDataObject(AiSession aiSession) {
        AiSessionDO dataObject = new AiSessionDO();
        dataObject.setId(aiSession.id());
        dataObject.setSessionUuid(aiSession.sessionUuid());
        dataObject.setPatientId(aiSession.patientId());
        dataObject.setDepartmentId(aiSession.departmentId());
        dataObject.setSceneType(aiSession.sceneType().name());
        dataObject.setEntrypoint(aiSession.entrypoint().name());
        dataObject.setSessionStatus(aiSession.status().name());
        dataObject.setChiefComplaintSummary(aiSession.chiefComplaintSummary());
        dataObject.setSummary(aiSession.summary());
        dataObject.setStartedAt(aiSession.startedAt());
        dataObject.setEndedAt(aiSession.endedAt());
        dataObject.setVersion(aiSession.version());
        return dataObject;
    }

    private AiSession toDomain(AiSessionDO dataObject) {
        return AiSession.rehydrate(
                dataObject.getId(),
                dataObject.getSessionUuid(),
                dataObject.getPatientId(),
                dataObject.getDepartmentId(),
                AiSceneType.valueOf(dataObject.getSceneType()),
                AiEntrypoint.valueOf(dataObject.getEntrypoint()),
                AiSessionStatus.valueOf(dataObject.getSessionStatus()),
                dataObject.getChiefComplaintSummary(),
                dataObject.getSummary(),
                dataObject.getStartedAt(),
                dataObject.getEndedAt(),
                dataObject.getVersion());
    }
}
