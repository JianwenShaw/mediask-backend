package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAiSessionTriageResultUseCase {

    private final AiSessionQueryRepository aiSessionQueryRepository;

    public GetAiSessionTriageResultUseCase(AiSessionQueryRepository aiSessionQueryRepository) {
        this.aiSessionQueryRepository = aiSessionQueryRepository;
    }

    @Transactional(readOnly = true)
    public AiSessionTriageResultView handle(GetAiSessionTriageResultQuery query) {
        AiSessionTriageResultView result = aiSessionQueryRepository
                .findLatestTriageResultBySessionId(query.sessionId())
                .orElse(null);
        if (result != null) {
            if (!result.patientId().equals(query.patientUserId())) {
                throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
            }
            return result;
        }

        if (!aiSessionQueryRepository.hasAccessibleTriageSession(query.patientUserId(), query.sessionId())) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }

        AiTriageStage latestTriageStage = aiSessionQueryRepository
                .findLatestTriageStageBySessionId(query.sessionId())
                .orElse(null);
        if (latestTriageStage == AiTriageStage.COLLECTING) {
            throw new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_READY);
        }
        throw new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_FOUND);
    }
}
