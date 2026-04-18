package me.jianwen.mediask.application.ai.usecase;

import java.time.Clock;
import java.time.LocalDate;
import me.jianwen.mediask.application.ai.query.GetAiSessionRegistrationHandoffQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionRegistrationHandoffView;
import org.springframework.transaction.annotation.Transactional;

public class GetAiSessionRegistrationHandoffUseCase {

    private static final String SUGGESTED_VISIT_TYPE_OUTPATIENT = "OUTPATIENT";
    private static final String BLOCKED_REASON_EMERGENCY_OFFLINE = "EMERGENCY_OFFLINE";

    private final AiRegistrationHandoffSupport aiRegistrationHandoffSupport;
    private final Clock clock;

    public GetAiSessionRegistrationHandoffUseCase(AiRegistrationHandoffSupport aiRegistrationHandoffSupport, Clock clock) {
        this.aiRegistrationHandoffSupport = aiRegistrationHandoffSupport;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiSessionRegistrationHandoffView handle(GetAiSessionRegistrationHandoffQuery query) {
        AiRegistrationHandoffSupport.ResolvedRegistrationHandoff handoff =
                aiRegistrationHandoffSupport.resolve(query.patientUserId(), query.sessionId());
        if (handoff.isBlockedForRegistration()) {
            return new AiSessionRegistrationHandoffView(
                    handoff.sessionId(),
                    handoff.patientId(),
                    null,
                    null,
                    handoff.chiefComplaintSummary(),
                    null,
                    BLOCKED_REASON_EMERGENCY_OFFLINE,
                    null);
        }

        handoff.requireRegistrationAvailable();
        LocalDate dateFrom = LocalDate.now(clock);
        return new AiSessionRegistrationHandoffView(
                handoff.sessionId(),
                handoff.patientId(),
                handoff.recommendedDepartment().departmentId(),
                handoff.recommendedDepartment().departmentName(),
                handoff.chiefComplaintSummary(),
                SUGGESTED_VISIT_TYPE_OUTPATIENT,
                null,
                new AiSessionRegistrationHandoffView.RegistrationQuery(
                        handoff.recommendedDepartment().departmentId(), dateFrom, dateFrom.plusDays(6)));
    }
}
