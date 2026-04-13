package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
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
                .orElseThrow(() -> new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_FOUND));
        if (!result.patientId().equals(query.patientUserId())) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }
        return result;
    }
}
