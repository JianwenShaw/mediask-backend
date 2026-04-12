package me.jianwen.mediask.infra.persistence.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiTurn;
import me.jianwen.mediask.domain.ai.model.AiTurnStatus;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import me.jianwen.mediask.infra.persistence.dataobject.AiTurnDO;
import me.jianwen.mediask.infra.persistence.mapper.AiTurnMapper;
import org.springframework.stereotype.Component;

@Component
public class AiTurnRepositoryAdapter implements AiTurnRepository {

    private final AiTurnMapper aiTurnMapper;

    public AiTurnRepositoryAdapter(AiTurnMapper aiTurnMapper) {
        this.aiTurnMapper = aiTurnMapper;
    }

    @Override
    public void save(AiTurn aiTurn) {
        aiTurnMapper.insert(toDataObject(aiTurn));
    }

    @Override
    public int findMaxTurnNoBySessionId(Long sessionId) {
        return aiTurnMapper.selectMaxTurnNoBySessionId(sessionId);
    }

    @Override
    public void update(AiTurn aiTurn) {
        AiTurnDO existing = aiTurnMapper.selectOne(Wrappers.lambdaQuery(AiTurnDO.class)
                .eq(AiTurnDO::getId, aiTurn.id())
                .isNull(AiTurnDO::getDeletedAt));
        if (existing == null) {
            throw new BizException(AiErrorCode.AI_TURN_UPDATE_CONFLICT);
        }

        AiTurnDO toUpdate = new AiTurnDO();
        toUpdate.setId(existing.getId());
        toUpdate.setVersion(existing.getVersion());
        toUpdate.setTurnStatus(aiTurn.status().name());
        toUpdate.setOutputHash(aiTurn.outputHash());
        toUpdate.setCompletedAt(aiTurn.completedAt());
        toUpdate.setErrorCode(aiTurn.errorCode());
        toUpdate.setErrorMessage(aiTurn.errorMessage());
        if (aiTurnMapper.updateById(toUpdate) != 1) {
            throw new BizException(AiErrorCode.AI_TURN_UPDATE_CONFLICT);
        }
    }

    private AiTurnDO toDataObject(AiTurn aiTurn) {
        AiTurnDO dataObject = new AiTurnDO();
        dataObject.setId(aiTurn.id());
        dataObject.setSessionId(aiTurn.sessionId());
        dataObject.setTurnNo(aiTurn.turnNo());
        dataObject.setTurnStatus(aiTurn.status().name());
        dataObject.setInputHash(aiTurn.inputHash());
        dataObject.setOutputHash(aiTurn.outputHash());
        dataObject.setStartedAt(aiTurn.startedAt());
        dataObject.setCompletedAt(aiTurn.completedAt());
        dataObject.setErrorCode(aiTurn.errorCode());
        dataObject.setErrorMessage(aiTurn.errorMessage());
        dataObject.setVersion(aiTurn.version());
        return dataObject;
    }
}
