package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.domain.ai.model.AiTriageGatewayContext;
import me.jianwen.mediask.domain.ai.model.AiTriageQuery;
import me.jianwen.mediask.domain.ai.model.AiTriageQueryResponse;
import me.jianwen.mediask.domain.ai.model.AiTriageRecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.AiTriageResult;
import me.jianwen.mediask.domain.ai.model.AiTriageResultSnapshot;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionSummaryList;
import me.jianwen.mediask.domain.ai.model.AiSessionTriageResult;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.ai.port.AiTriageResultSnapshotRepository;
import me.jianwen.mediask.domain.triage.model.CatalogVersion;
import me.jianwen.mediask.domain.triage.model.DepartmentCandidate;
import me.jianwen.mediask.domain.triage.model.TriageCatalog;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import org.junit.jupiter.api.Test;

class StreamAiTriageQueryUseCaseTest {

    @Test
    void handle_WhenFinalEventArrives_WritesFramesAndPersistsSnapshot() throws Exception {
        CapturingSnapshotRepository snapshotRepository = new CapturingSnapshotRepository();
        SubmitAiTriageQueryUseCase submitUseCase = new SubmitAiTriageQueryUseCase(
                new NoopGatewayPort(),
                snapshotRepository,
                new StubCatalogPublishPort());
        StreamAiTriageQueryUseCase useCase = new StreamAiTriageQueryUseCase(new StreamingGatewayPort(), submitUseCase);
        java.util.LinkedList<AiTriageGatewayPort.StreamEvent> events = new java.util.LinkedList<>();

        useCase.handle(new SubmitAiTriageQueryCommand("req-1", 2201L, null, "default", "头痛"), new StreamAiTriageQueryUseCase.StreamEventWriter() {
            @Override
            public void writeEvent(AiTriageGatewayPort.StreamEvent event) {
                events.add(event);
            }

            @Override
            public void writeError(String code, String message) {
                throw new AssertionError("unexpected error event");
            }
        });

        assertEquals(3, events.size());
        assertEquals("start", events.get(0).event());
        assertTrue(events.get(1).isFinal());
        assertEquals(1, snapshotRepository.savedSnapshots.size());
        assertEquals("query-1", snapshotRepository.savedSnapshots.getFirst().queryRunId());
    }

    private static final class StreamingGatewayPort implements AiTriageGatewayPort {

        @Override
        public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void streamQuery(AiTriageGatewayContext context, AiTriageQuery query, StreamEventHandler handler) {
            handler.onEvent(new StreamEvent("start", "{\"request_id\":\"req-1\"}", null));
            handler.onEvent(new StreamEvent(
                    "final",
                    "{\"request_id\":\"req-1\",\"session_id\":\"session-1\",\"turn_id\":\"turn-1\",\"query_run_id\":\"query-1\",\"triage_result\":{\"triage_stage\":\"READY\",\"triage_completion_reason\":\"SUFFICIENT_INFO\",\"next_action\":\"VIEW_TRIAGE_RESULT\",\"risk_level\":\"low\",\"chief_complaint_summary\":\"头痛\",\"recommended_departments\":[{\"department_id\":3101,\"department_name\":\"神经内科\",\"priority\":1,\"reason\":\"头痛优先神经内科\"}],\"care_advice\":\"建议门诊就诊\",\"catalog_version\":\"deptcat-v20260501-01\",\"citations\":[]}}",
                    new AiTriageQueryResponse(
                            "req-1",
                            "session-1",
                            "turn-1",
                            "query-1",
                            new AiTriageResult(
                                    "READY",
                                    "SUFFICIENT_INFO",
                                    "VIEW_TRIAGE_RESULT",
                                    "low",
                                    "头痛",
                                    List.of(),
                                    List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                                    "建议门诊就诊",
                                    null,
                                    "deptcat-v20260501-01",
                                    List.of()))));
            handler.onEvent(new StreamEvent("done", "{}", null));
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
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoopGatewayPort implements AiTriageGatewayPort {

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
            throw new UnsupportedOperationException();
        }
    }

    private static final class CapturingSnapshotRepository implements AiTriageResultSnapshotRepository {

        private final java.util.LinkedList<AiTriageResultSnapshot> savedSnapshots = new java.util.LinkedList<>();

        @Override
        public void save(AiTriageResultSnapshot snapshot) {
            savedSnapshots.add(snapshot);
        }
    }

    private static final class StubCatalogPublishPort implements TriageCatalogPublishPort {

        @Override
        public void publish(TriageCatalog catalog) {
        }

        @Override
        public Optional<TriageCatalog> findActiveCatalog(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
            return Optional.of(new TriageCatalog(
                    "default",
                    version,
                    OffsetDateTime.parse("2026-05-01T10:00:00+08:00"),
                    List.of(new DepartmentCandidate(3101L, "神经内科", "头痛头晕", List.of(), 10))));
        }

        @Override
        public Optional<CatalogVersion> findActiveVersion(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public CatalogVersion nextVersion(String hospitalScope) {
            return new CatalogVersion("deptcat-v20260501-01");
        }
    }
}
