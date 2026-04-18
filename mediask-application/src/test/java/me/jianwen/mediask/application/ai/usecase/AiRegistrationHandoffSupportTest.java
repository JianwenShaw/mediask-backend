package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionListItem;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResultView;
import me.jianwen.mediask.domain.ai.model.AiTriageResultStatus;
import me.jianwen.mediask.domain.ai.model.AiTriageStage;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.junit.jupiter.api.Test;

class AiRegistrationHandoffSupportTest {

    @Test
    void resolve_WhenRecommendedDepartmentExists_ReturnFirstByPriority() {
        AiRegistrationHandoffSupport support = new AiRegistrationHandoffSupport(new ReadyQueryRepository());

        AiRegistrationHandoffSupport.ResolvedRegistrationHandoff result = support.resolve(1001L, 9001L);

        assertEquals(1001L, result.patientId());
        assertEquals(101L, result.recommendedDepartment().departmentId());
        assertEquals("神经内科", result.recommendedDepartment().departmentName());
    }

    @Test
    void resolve_WhenHighRisk_ReturnBlockedResolution() {
        AiRegistrationHandoffSupport support = new AiRegistrationHandoffSupport(new HighRiskQueryRepository());

        AiRegistrationHandoffSupport.ResolvedRegistrationHandoff result = support.resolve(1001L, 9001L);

        assertEquals(true, result.isBlockedForRegistration());
        BizException exception = assertThrows(BizException.class, result::requireRegistrationAvailable);
        assertEquals(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    void resolve_WhenNoRecommendedDepartment_ReturnResolutionThatRejectsRegistration() {
        AiRegistrationHandoffSupport support = new AiRegistrationHandoffSupport(new NoDepartmentQueryRepository());

        AiRegistrationHandoffSupport.ResolvedRegistrationHandoff result = support.resolve(1001L, 9001L);

        BizException exception = assertThrows(BizException.class, result::requireRegistrationAvailable);
        assertEquals(AiErrorCode.AI_SESSION_REGISTRATION_HANDOFF_UNAVAILABLE.getCode(), exception.getCode());
    }

    @Test
    void resolve_WhenTriageStillCollecting_ThrowNotReady() {
        AiRegistrationHandoffSupport support = new AiRegistrationHandoffSupport(new CollectingOnlyQueryRepository());

        BizException exception = assertThrows(BizException.class, () -> support.resolve(1001L, 9001L));

        assertEquals(AiErrorCode.AI_SESSION_TRIAGE_RESULT_NOT_READY.getCode(), exception.getCode());
    }

    @Test
    void resolve_WhenPatientCannotAccessSession_ThrowAccessDenied() {
        AiRegistrationHandoffSupport support = new AiRegistrationHandoffSupport(new ReadyQueryRepository());

        BizException exception = assertThrows(BizException.class, () -> support.resolve(9999L, 9001L));

        assertEquals(AiErrorCode.AI_SESSION_ACCESS_DENIED.getCode(), exception.getCode());
    }

    private static class ReadyQueryRepository implements AiSessionQueryRepository {
        @Override
        public List<AiSessionListItem> listSessionsByPatientUserId(Long patientUserId) {
            return List.of();
        }

        @Override
        public Optional<AiSessionDetail> findSessionDetailById(Long sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    9001L,
                    1001L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.READY,
                    9101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    List.of(
                            new RecommendedDepartment(102L, "全科", 2, "普通评估"),
                            new RecommendedDepartment(101L, "神经内科", 1, "持续头痛")),
                    "建议线下就诊",
                    List.of(new AiCitation(7001L, 1, 0.82D, "持续头痛建议线下评估"))));
        }

        @Override
        public Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
            return Optional.of(AiTriageStage.READY);
        }

        @Override
        public boolean hasAccessibleTriageSession(Long patientUserId, Long sessionId) {
            return patientUserId.equals(1001L) && sessionId.equals(9001L);
        }
    }

    private static final class HighRiskQueryRepository extends ReadyQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    9001L,
                    1001L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.BLOCKED,
                    9101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "胸痛一小时",
                    RiskLevel.HIGH,
                    GuardrailAction.CAUTION,
                    List.of(new RecommendedDepartment(101L, "急诊科", 1, "胸痛高风险")),
                    "立即线下就医",
                    List.of()));
        }
    }

    private static final class NoDepartmentQueryRepository extends ReadyQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.of(new AiSessionTriageResultView(
                    9001L,
                    1001L,
                    AiTriageResultStatus.CURRENT,
                    AiTriageStage.READY,
                    9101L,
                    OffsetDateTime.parse("2026-04-12T09:31:00+08:00"),
                    false,
                    null,
                    "头痛三天",
                    RiskLevel.MEDIUM,
                    GuardrailAction.CAUTION,
                    List.of(),
                    "建议线下就诊",
                    List.of()));
        }
    }

    private static final class CollectingOnlyQueryRepository extends ReadyQueryRepository {
        @Override
        public Optional<AiSessionTriageResultView> findLatestTriageResultBySessionId(Long sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<AiTriageStage> findLatestTriageStageBySessionId(Long sessionId) {
            return Optional.of(AiTriageStage.COLLECTING);
        }
    }
}
