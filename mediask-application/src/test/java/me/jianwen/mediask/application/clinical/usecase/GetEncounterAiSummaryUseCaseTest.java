package me.jianwen.mediask.application.clinical.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.TestAuditSupport;
import me.jianwen.mediask.application.clinical.query.GetEncounterAiSummaryQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.common.exception.ErrorCode;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.clinical.exception.ClinicalErrorCode;
import me.jianwen.mediask.domain.clinical.model.EncounterAiSummary;
import me.jianwen.mediask.domain.clinical.model.EncounterDetail;
import me.jianwen.mediask.domain.clinical.model.EncounterListItem;
import me.jianwen.mediask.domain.clinical.model.EncounterPatientSummary;
import me.jianwen.mediask.domain.clinical.model.VisitEncounterStatus;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import me.jianwen.mediask.domain.outpatient.model.RegistrationDetail;
import me.jianwen.mediask.domain.outpatient.model.RegistrationListItem;
import me.jianwen.mediask.domain.outpatient.model.RegistrationStatus;
import me.jianwen.mediask.domain.outpatient.port.RegistrationOrderQueryRepository;
import org.junit.jupiter.api.Test;

class GetEncounterAiSummaryUseCaseTest {

    @Test
    void handle_WhenEncounterHasAiSession_ReturnStructuredSummary() {
        StubAiTriageGatewayPort aiTriageGatewayPort = new StubAiTriageGatewayPort();
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(
                new StubEncounterQueryRepository(),
                new StubRegistrationOrderQueryRepository(),
                aiTriageGatewayPort,
                TestAuditSupport.auditTrailService());

        EncounterAiSummary result = useCase.handle(
                new GetEncounterAiSummaryQuery(8101L, 2101L),
                TestAuditSupport.auditContext());

        assertEquals(8101L, result.encounterId());
        assertEquals("session-1", aiTriageGatewayPort.lastSessionId);
        assertEquals(2003L, aiTriageGatewayPort.lastContext.actorUserId());
        assertEquals("头痛", result.chiefComplaintSummary());
        assertEquals("low", result.riskLevel());
        assertEquals("chunk-1", result.citations().getFirst().chunkId());
    }

    @Test
    void handle_WhenNoSourceAiSession_ThrowNotFound() {
        StubRegistrationOrderQueryRepository registrationOrderQueryRepository = new StubRegistrationOrderQueryRepository();
        registrationOrderQueryRepository.sourceAiSessionId = Optional.empty();
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(
                new StubEncounterQueryRepository(),
                registrationOrderQueryRepository,
                new StubAiTriageGatewayPort(),
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenTriageResultMissing_TranslateToEncounterAiSummaryNotFound() {
        StubAiTriageGatewayPort aiTriageGatewayPort = new StubAiTriageGatewayPort();
        aiTriageGatewayPort.throwable = new BizException(ErrorCode.RESOURCE_NOT_FOUND);
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(
                new StubEncounterQueryRepository(),
                new StubRegistrationOrderQueryRepository(),
                aiTriageGatewayPort,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenTriageResultNotReady_TranslateToEncounterAiSummaryNotFound() {
        StubAiTriageGatewayPort aiTriageGatewayPort = new StubAiTriageGatewayPort();
        aiTriageGatewayPort.throwable = new BizException(AiErrorCode.TRIAGE_RESULT_NOT_READY);
        GetEncounterAiSummaryUseCase useCase = new GetEncounterAiSummaryUseCase(
                new StubEncounterQueryRepository(),
                new StubRegistrationOrderQueryRepository(),
                aiTriageGatewayPort,
                TestAuditSupport.auditTrailService());

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new GetEncounterAiSummaryQuery(8101L, 2101L), TestAuditSupport.auditContext()));

        assertEquals(ClinicalErrorCode.ENCOUNTER_AI_SUMMARY_NOT_FOUND.getCode(), exception.getCode());
    }

    private static final class StubEncounterQueryRepository implements EncounterQueryRepository {

        @Override
        public List<EncounterListItem> listByDoctorId(Long doctorId, VisitEncounterStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<EncounterDetail> findDetailByEncounterId(Long encounterId) {
            return Optional.of(new EncounterDetail(
                    encounterId,
                    6101L,
                    2101L,
                    new EncounterPatientSummary(
                            2003L,
                            "李患者",
                            "FEMALE",
                            3101L,
                            "心内科",
                            LocalDate.parse("2026-04-03"),
                            "MORNING",
                            VisitEncounterStatus.SCHEDULED,
                            OffsetDateTime.parse("2026-04-03T09:00:00+08:00"),
                            null,
                            LocalDate.parse("1990-05-15"))));
        }
    }

    private static final class StubRegistrationOrderQueryRepository implements RegistrationOrderQueryRepository {

        private Optional<String> sourceAiSessionId = Optional.of("session-1");

        @Override
        public List<RegistrationListItem> listByPatientUserId(Long patientUserId, RegistrationStatus status) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RegistrationDetail> findDetailByPatientUserIdAndRegistrationId(Long patientUserId, Long registrationId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<String> findSourceAiSessionIdByRegistrationId(Long registrationId) {
            return sourceAiSessionId;
        }
    }

    private static final class StubAiTriageGatewayPort implements AiTriageGatewayPort {

        private AiTriageGatewayContext lastContext;
        private String lastSessionId;
        private RuntimeException throwable;

        @Override
        public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiSessionSummaryList listSessions(AiTriageGatewayContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiSessionDetail getSessionDetail(AiTriageGatewayContext context, String sessionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AiSessionTriageResult getSessionTriageResult(AiTriageGatewayContext context, String sessionId) {
            this.lastContext = context;
            this.lastSessionId = sessionId;
            if (throwable != null) {
                throw throwable;
            }
            return new AiSessionTriageResult(
                    sessionId,
                    "CURRENT",
                    "READY",
                    "low",
                    "allow",
                    "VIEW_TRIAGE_RESULT",
                    "turn-1",
                    OffsetDateTime.parse("2026-05-01T09:03:00+08:00"),
                    false,
                    null,
                    "头痛",
                    List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                    "建议门诊就诊",
                    List.of(new AiTriageCitation(1, "chunk-1", "头痛优先神经内科")),
                    null,
                    "deptcat-v20260501-01");
        }
    }
}
