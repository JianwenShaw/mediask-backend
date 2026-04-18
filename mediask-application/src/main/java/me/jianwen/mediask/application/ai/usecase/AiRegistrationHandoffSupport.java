package me.jianwen.mediask.application.ai.usecase;

import java.util.Comparator;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;

public class AiRegistrationHandoffSupport {

    private final AiSessionQueryRepository aiSessionQueryRepository;

    public AiRegistrationHandoffSupport(AiSessionQueryRepository aiSessionQueryRepository) {
        this.aiSessionQueryRepository = aiSessionQueryRepository;
    }

    public ResolvedRegistrationHandoff resolve(Long patientUserId, Long sessionId) {
        AiSessionTriageResultView triageResult =
                aiSessionQueryRepository.findLatestTriageResultBySessionId(sessionId).orElse(null);
        if (triageResult != null) {
            if (!triageResult.patientId().equals(patientUserId)) {
                throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
            }
            return new ResolvedRegistrationHandoff(
                    triageResult.sessionId(),
                    triageResult.patientId(),
                    triageResult.chiefComplaintSummary(),
                    triageResult.riskLevel(),
                    triageResult.recommendedDepartments().stream()
                            .sorted(Comparator.comparing(
                                    RecommendedDepartment::priority, Comparator.nullsLast(Integer::compareTo)))
                            .findFirst()
                            .orElse(null));
        }

        if (!aiSessionQueryRepository.hasAccessibleTriageSession(patientUserId, sessionId)) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }

        AiTriageStage latestTriageStage =
                aiSessionQueryRepository.findLatestTriageStageBySessionId(sessionId).orElse(null);
        if (latestTriageStage == AiTriageStage.COLLECTING) {
            throw new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_READY);
        }
        throw new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_FOUND);
    }

    public record ResolvedRegistrationHandoff(
            Long sessionId,
            Long patientId,
            String chiefComplaintSummary,
            RiskLevel riskLevel,
            RecommendedDepartment recommendedDepartment) {

        public boolean isBlockedForRegistration() {
            return riskLevel == RiskLevel.HIGH;
        }

        public void requireRegistrationAvailable() {
            if (isBlockedForRegistration() || recommendedDepartment == null) {
                throw new BizException(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE);
            }
        }
    }
}
