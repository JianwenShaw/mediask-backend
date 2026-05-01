package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.application.ai.query.GetAiSessionTriageResultQuery;
import me.jianwen.mediask.application.ai.query.ListAiSessionsQuery;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionSummary;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.model.AiSessionTurn;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import org.junit.jupiter.api.Test;

class AiSessionQueryUseCaseTest {

    @Test
    void listSessions_PassesRequestIdAndPatientUserIdToGateway() {
        CapturingGatewayPort gatewayPort = new CapturingGatewayPort();

        AiSessionSummaryList response = new ListAiSessionsUseCase(gatewayPort)
                .handle(new ListAiSessionsQuery("req-1", 2201L));

        assertEquals("req-1", gatewayPort.lastContext.requestId());
        assertEquals(2201L, gatewayPort.lastContext.actorUserId());
        assertEquals("session-1", response.items().getFirst().sessionId());
    }

    @Test
    void getSessionDetail_PassesSessionIdToGateway() {
        CapturingGatewayPort gatewayPort = new CapturingGatewayPort();

        AiSessionDetail response = new GetAiSessionDetailUseCase(gatewayPort)
                .handle(new GetAiSessionDetailQuery("req-2", 2201L, "session-2"));

        assertEquals("req-2", gatewayPort.lastContext.requestId());
        assertEquals("session-2", gatewayPort.lastSessionId);
        assertEquals("session-2", response.sessionId());
    }

    @Test
    void getSessionTriageResult_PassesSessionIdToGateway() {
        CapturingGatewayPort gatewayPort = new CapturingGatewayPort();

        AiSessionTriageResult response = new GetAiSessionTriageResultUseCase(gatewayPort)
                .handle(new GetAiSessionTriageResultQuery("req-3", 2201L, "session-3"));

        assertEquals("req-3", gatewayPort.lastContext.requestId());
        assertEquals("session-3", gatewayPort.lastSessionId);
        assertEquals("READY", response.triageStage());
    }

    private static final class CapturingGatewayPort implements AiTriageGatewayPort {

        private AiTriageGatewayContext lastContext;
        private String lastSessionId;

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
            this.lastContext = context;
            return new AiSessionSummaryList(List.of(new AiSessionSummary(
                    "session-1",
                    "AI_TRIAGE",
                    "COLLECTING",
                    3101L,
                    "头痛",
                    "建议继续补充症状",
                    OffsetDateTime.parse("2026-05-01T09:00:00+08:00"),
                    OffsetDateTime.parse("2026-05-01T09:03:00+08:00"))));
        }

        @Override
        public AiSessionDetail getSessionDetail(AiTriageGatewayContext context, String sessionId) {
            this.lastContext = context;
            this.lastSessionId = sessionId;
            return new AiSessionDetail(
                    sessionId,
                    "AI_TRIAGE",
                    "COLLECTING",
                    3101L,
                    "头痛",
                    "建议继续补充症状",
                    OffsetDateTime.parse("2026-05-01T09:00:00+08:00"),
                    OffsetDateTime.parse("2026-05-01T09:03:00+08:00"),
                    List.of(new AiSessionTurn(
                            "turn-1",
                            1,
                            "COLLECTING",
                            OffsetDateTime.parse("2026-05-01T09:00:00+08:00"),
                            OffsetDateTime.parse("2026-05-01T09:00:05+08:00"),
                            null,
                            null,
                            List.of(new AiSessionMessage(
                                    "user",
                                    "头痛",
                                    OffsetDateTime.parse("2026-05-01T09:00:00+08:00"))))));
        }

        @Override
        public AiSessionTriageResult getSessionTriageResult(AiTriageGatewayContext context, String sessionId) {
            this.lastContext = context;
            this.lastSessionId = sessionId;
            return new AiSessionTriageResult(
                    sessionId,
                    "CURRENT",
                    "READY",
                    "low",
                    "allow",
                    "VIEW_TRIAGE_RESULT",
                    "turn-2",
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
