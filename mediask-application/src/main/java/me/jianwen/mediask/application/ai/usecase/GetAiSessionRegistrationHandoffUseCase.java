package me.jianwen.mediask.application.ai.usecase;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import me.jianwen.mediask.application.ai.query.GetAiSessionRegistrationHandoffQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionRegistrationHandoffView;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAiSessionRegistrationHandoffUseCase {

    private static final String SUGGESTED_VISIT_TYPE_OUTPATIENT = "OUTPATIENT";
    private static final String BLOCKED_REASON_EMERGENCY_OFFLINE = "EMERGENCY_OFFLINE";

    private final AiSessionQueryRepository aiSessionQueryRepository;
    private final Clock clock;

    public GetAiSessionRegistrationHandoffUseCase(AiSessionQueryRepository aiSessionQueryRepository, Clock clock) {
        this.aiSessionQueryRepository = aiSessionQueryRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiSessionRegistrationHandoffView handle(GetAiSessionRegistrationHandoffQuery query) {
        AiSessionTriageResultView triageResult = aiSessionQueryRepository
                .findLatestTriageResultBySessionId(query.sessionId())
                .orElseThrow(() -> new BizException(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_FOUND));
        if (!triageResult.patientId().equals(query.patientUserId())) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }
        if (triageResult.riskLevel() == RiskLevel.HIGH) {
            return new AiSessionRegistrationHandoffView(
                    triageResult.sessionId(),
                    triageResult.patientId(),
                    null,
                    null,
                    triageResult.chiefComplaintSummary(),
                    null,
                    BLOCKED_REASON_EMERGENCY_OFFLINE,
                    null);
        }

        RecommendedDepartment department = triageResult.recommendedDepartments().stream()
                .sorted(Comparator.comparing(RecommendedDepartment::priority, Comparator.nullsLast(Integer::compareTo)))
                .findFirst()
                .orElseThrow(() -> new BizException(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE));

        LocalDate dateFrom = LocalDate.now(clock);
        return new AiSessionRegistrationHandoffView(
                triageResult.sessionId(),
                triageResult.patientId(),
                department.departmentId(),
                department.departmentName(),
                triageResult.chiefComplaintSummary(),
                SUGGESTED_VISIT_TYPE_OUTPATIENT,
                null,
                new AiSessionRegistrationHandoffView.RegistrationQuery(
                        department.departmentId(), dateFrom, dateFrom.plusDays(6)));
    }
}
