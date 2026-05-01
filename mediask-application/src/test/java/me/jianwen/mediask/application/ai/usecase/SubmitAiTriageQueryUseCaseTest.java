package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.SubmitAiTriageQueryCommand;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.model.AiTriageCitation;
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

class SubmitAiTriageQueryUseCaseTest {

    @Test
    void handle_WhenCollecting_DoesNotPersistSnapshot() {
        StubGatewayPort gatewayPort = new StubGatewayPort(collectingResponse());
        CapturingSnapshotRepository snapshotRepository = new CapturingSnapshotRepository();
        SubmitAiTriageQueryUseCase useCase = new SubmitAiTriageQueryUseCase(
                gatewayPort,
                snapshotRepository,
                new StubCatalogPublishPort(Optional.empty()));

        AiTriageQueryResponse response = useCase.handle(command());

        assertEquals("COLLECTING", response.triageResult().triageStage());
        assertTrue(snapshotRepository.savedSnapshots.isEmpty());
    }

    @Test
    void handle_WhenReady_PersistsFinalizedSnapshot() {
        StubGatewayPort gatewayPort = new StubGatewayPort(readyResponse());
        CapturingSnapshotRepository snapshotRepository = new CapturingSnapshotRepository();
        SubmitAiTriageQueryUseCase useCase = new SubmitAiTriageQueryUseCase(
                gatewayPort,
                snapshotRepository,
                new StubCatalogPublishPort(Optional.of(defaultCatalog())));

        AiTriageQueryResponse response = useCase.handle(command());

        assertEquals("READY", response.triageResult().triageStage());
        assertEquals(1, snapshotRepository.savedSnapshots.size());
        assertEquals("query-1", snapshotRepository.savedSnapshots.getFirst().queryRunId());
    }

    @Test
    void handle_WhenReadyCatalogMissing_ThrowsAiResponseInvalid() {
        StubGatewayPort gatewayPort = new StubGatewayPort(readyResponse());
        SubmitAiTriageQueryUseCase useCase = new SubmitAiTriageQueryUseCase(
                gatewayPort,
                new CapturingSnapshotRepository(),
                new StubCatalogPublishPort(Optional.empty()));

        SysException exception = assertThrows(SysException.class, () -> useCase.handle(command()));

        assertEquals(6003, exception.getCode());
    }

    private SubmitAiTriageQueryCommand command() {
        return new SubmitAiTriageQueryCommand("req-1", 2201L, null, "default", "头痛两天");
    }

    private AiTriageQueryResponse collectingResponse() {
        return new AiTriageQueryResponse(
                "req-1",
                "session-1",
                "turn-1",
                "query-1",
                new AiTriageResult(
                        "COLLECTING",
                        null,
                        "CONTINUE_TRIAGE",
                        null,
                        "头痛两天",
                        List.of("请问是否有发热？"),
                        List.of(),
                        null,
                        null,
                        null,
                        List.of()));
    }

    private AiTriageQueryResponse readyResponse() {
        return new AiTriageQueryResponse(
                "req-1",
                "session-1",
                "turn-1",
                "query-1",
                new AiTriageResult(
                        "READY",
                        "SUFFICIENT_INFO",
                        "VIEW_TRIAGE_RESULT",
                        "low",
                        "头痛两天",
                        List.of(),
                        List.of(new AiTriageRecommendedDepartment(3101L, "神经内科", 1, "头痛优先神经内科")),
                        "建议门诊就诊",
                        null,
                        "deptcat-v20260501-01",
                        List.of(new AiTriageCitation(1, "chunk-1", "头痛优先神经内科就诊"))));
    }

    private TriageCatalog defaultCatalog() {
        return new TriageCatalog(
                "default",
                new CatalogVersion("deptcat-v20260501-01"),
                OffsetDateTime.parse("2026-05-01T10:00:00+08:00"),
                List.of(new DepartmentCandidate(3101L, "神经内科", "头痛头晕", List.of("神内"), 10)));
    }

    private static final class StubGatewayPort implements AiTriageGatewayPort {

        private final AiTriageQueryResponse response;

        private StubGatewayPort(AiTriageQueryResponse response) {
            this.response = response;
        }

        @Override
        public AiTriageQueryResponse query(AiTriageGatewayContext context, AiTriageQuery query) {
            return response;
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

        private final Optional<TriageCatalog> catalog;

        private StubCatalogPublishPort(Optional<TriageCatalog> catalog) {
            this.catalog = catalog;
        }

        @Override
        public void publish(TriageCatalog catalog) {
        }

        @Override
        public Optional<TriageCatalog> findActiveCatalog(String hospitalScope) {
            return Optional.empty();
        }

        @Override
        public Optional<TriageCatalog> findCatalogByVersion(String hospitalScope, CatalogVersion version) {
            return catalog;
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
