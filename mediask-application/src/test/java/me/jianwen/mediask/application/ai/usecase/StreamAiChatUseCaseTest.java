package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.AiExecutionMetadata;
import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;
import me.jianwen.mediask.domain.ai.model.AiModelRun;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.AiSession;
import me.jianwen.mediask.domain.ai.model.AiTurn;
import me.jianwen.mediask.domain.ai.model.AiTurnContent;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.junit.jupiter.api.Test;

class StreamAiChatUseCaseTest {

    @Test
    void handle_WhenNewSessionSucceeds_ShouldPersistConversationAndEmitMetaThenEnd() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiTurnContentRepository turnContentRepository = new InMemoryAiTurnContentRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        InMemoryAiGuardrailEventRepository guardrailEventRepository = new InMemoryAiGuardrailEventRepository();
        StubAiChatStreamPort streamPort = new StubAiChatStreamPort();
        streamPort.eventPublisher = consumer -> {
            consumer.accept(new AiChatStreamEvent.Message("建议"));
            consumer.accept(new AiChatStreamEvent.Message("挂神经内科"));
            consumer.accept(new AiChatStreamEvent.Meta(successTriageResult()));
            consumer.accept(new AiChatStreamEvent.End());
        };
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                streamPort,
                sessionRepository,
                turnRepository,
                turnContentRepository,
                modelRunRepository,
                guardrailEventRepository,
                encryptor());

        List<AiChatStreamResultEvent> events = new ArrayList<>();
        useCase.handle(command(1001L, null), events::add);

        assertEquals(1, sessionRepository.store.size());
        assertEquals(1, turnRepository.store.size());
        assertEquals(2, turnContentRepository.savedContents.size());
        assertEquals("enc<头痛三天>", turnContentRepository.savedContents.get(0).encryptedContent());
        assertEquals("enc<建议挂神经内科>", turnContentRepository.savedContents.get(1).encryptedContent());
        assertEquals(1, modelRunRepository.store.size());
        assertEquals(1, guardrailEventRepository.savedEvents.size());
        assertEquals(4, events.size());
        assertInstanceOf(AiChatStreamResultEvent.Message.class, events.get(0));
        assertInstanceOf(AiChatStreamResultEvent.Message.class, events.get(1));
        assertInstanceOf(AiChatStreamResultEvent.Meta.class, events.get(2));
        assertInstanceOf(AiChatStreamResultEvent.End.class, events.get(3));
    }

    @Test
    void handle_WhenSessionBelongsToAnotherPatient_ShouldReject() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        AiSession existing = AiSession.createActive(2002L, 3001L, AiSceneType.PRE_CONSULTATION);
        sessionRepository.save(existing);
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                new StubAiChatStreamPort(),
                sessionRepository,
                new InMemoryAiTurnRepository(),
                new InMemoryAiTurnContentRepository(),
                new InMemoryAiModelRunRepository(),
                new InMemoryAiGuardrailEventRepository(),
                encryptor());

        BizException exception =
                assertThrows(BizException.class, () -> useCase.handle(command(1001L, existing.id()), event -> {}));

        assertEquals(AiErrorCode.AI_SESSION_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenUpstreamReturnsError_ShouldMarkTurnAndRunFailed() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        StubAiChatStreamPort streamPort = new StubAiChatStreamPort();
        streamPort.eventPublisher = consumer -> consumer.accept(new AiChatStreamEvent.Error(6001, "ai service unavailable"));
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                streamPort,
                sessionRepository,
                turnRepository,
                new InMemoryAiTurnContentRepository(),
                modelRunRepository,
                new InMemoryAiGuardrailEventRepository(),
                encryptor());

        List<AiChatStreamResultEvent> events = new ArrayList<>();
        useCase.handle(command(1001L, null), events::add);

        assertTrue(turnRepository.lastUpdated.errorCode() != null);
        assertTrue(modelRunRepository.lastUpdated.errorCode() != null);
        assertInstanceOf(AiChatStreamResultEvent.Error.class, events.getFirst());
    }

    @Test
    void handle_WhenMetaMissing_ShouldReturnInvalidResponseErrorAndMarkFailed() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        StubAiChatStreamPort streamPort = new StubAiChatStreamPort();
        streamPort.eventPublisher = consumer -> {
            consumer.accept(new AiChatStreamEvent.Message("建议"));
            consumer.accept(new AiChatStreamEvent.End());
        };
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                streamPort,
                sessionRepository,
                turnRepository,
                new InMemoryAiTurnContentRepository(),
                modelRunRepository,
                new InMemoryAiGuardrailEventRepository(),
                encryptor());

        List<AiChatStreamResultEvent> events = new ArrayList<>();
        useCase.handle(command(1001L, null), events::add);

        AiChatStreamResultEvent.Error error = assertInstanceOf(AiChatStreamResultEvent.Error.class, events.getLast());
        assertEquals(AiErrorCode.INVALID_RESPONSE.getCode(), error.code());
        assertEquals(AiErrorCode.INVALID_RESPONSE.getCode(), turnRepository.lastUpdated.errorCode());
        assertEquals(AiErrorCode.INVALID_RESPONSE.getCode(), modelRunRepository.lastUpdated.errorCode());
    }

    @Test
    void handle_WhenMetaDuplicated_ShouldReturnInvalidResponseError() {
        StubAiChatStreamPort streamPort = new StubAiChatStreamPort();
        streamPort.eventPublisher = consumer -> {
            consumer.accept(new AiChatStreamEvent.Meta(successTriageResult()));
            consumer.accept(new AiChatStreamEvent.Meta(successTriageResult()));
        };
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                streamPort,
                new InMemoryAiSessionRepository(),
                new InMemoryAiTurnRepository(),
                new InMemoryAiTurnContentRepository(),
                new InMemoryAiModelRunRepository(),
                new InMemoryAiGuardrailEventRepository(),
                encryptor());

        List<AiChatStreamResultEvent> events = new ArrayList<>();
        useCase.handle(command(1001L, null), events::add);

        AiChatStreamResultEvent.Error error = assertInstanceOf(AiChatStreamResultEvent.Error.class, events.getLast());
        assertEquals(AiErrorCode.INVALID_RESPONSE.getCode(), error.code());
    }

    @Test
    void handle_WhenTransportThrows_ShouldMarkFailedAndEmitError() {
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        StubAiChatStreamPort streamPort = new StubAiChatStreamPort();
        streamPort.failure = new me.jianwen.mediask.common.exception.SysException(AiErrorCode.SERVICE_TIMEOUT);
        StreamAiChatUseCase useCase = new StreamAiChatUseCase(
                streamPort,
                new InMemoryAiSessionRepository(),
                turnRepository,
                new InMemoryAiTurnContentRepository(),
                modelRunRepository,
                new InMemoryAiGuardrailEventRepository(),
                encryptor());

        List<AiChatStreamResultEvent> events = new ArrayList<>();
        useCase.handle(command(1001L, null), events::add);

        AiChatStreamResultEvent.Error error = assertInstanceOf(AiChatStreamResultEvent.Error.class, events.getFirst());
        assertEquals(AiErrorCode.SERVICE_TIMEOUT.getCode(), error.code());
        assertEquals(AiErrorCode.SERVICE_TIMEOUT.getCode(), turnRepository.lastUpdated.errorCode());
        assertEquals(AiErrorCode.SERVICE_TIMEOUT.getCode(), modelRunRepository.lastUpdated.errorCode());
    }

    private StreamAiChatCommand command(Long patientUserId, Long sessionId) {
        return new StreamAiChatCommand(
                patientUserId,
                sessionId,
                "头痛三天",
                2001L,
                AiSceneType.PRE_CONSULTATION,
                "req_stream_001");
    }

    private static AiContentEncryptorPort encryptor() {
        return plainText -> "enc<" + plainText + ">";
    }

    private AiChatTriageResult successTriageResult() {
        return new AiChatTriageResult(
                "头痛三天",
                RiskLevel.MEDIUM,
                GuardrailAction.CAUTION,
                List.of(new RecommendedDepartment(101L, "神经内科", 1, "头痛持续")),
                "建议线下就诊",
                List.of(new AiCitation(7001L, 1, 0.82D, "持续头痛建议线下评估")),
                new AiExecutionMetadata("provider-run-1", List.of("RISK_HEADACHE"), 100, 200, 1234, false));
    }

    private static final class StubAiChatStreamPort implements AiChatStreamPort {
        private Consumer<Consumer<AiChatStreamEvent>> eventPublisher = consumer -> {};
        private RuntimeException failure;

        @Override
        public void stream(
                me.jianwen.mediask.domain.ai.model.AiChatInvocation invocation,
                Consumer<AiChatStreamEvent> eventConsumer) {
            if (failure != null) {
                throw failure;
            }
            eventPublisher.accept(eventConsumer);
        }
    }

    private static final class InMemoryAiSessionRepository implements AiSessionRepository {
        private final Map<Long, AiSession> store = new HashMap<>();

        @Override
        public void save(AiSession aiSession) {
            store.put(aiSession.id(), aiSession);
        }

        @Override
        public Optional<AiSession> findById(Long sessionId) {
            return Optional.ofNullable(store.get(sessionId));
        }

        @Override
        public void update(AiSession aiSession) {
            store.put(aiSession.id(), aiSession);
        }
    }

    private static final class InMemoryAiTurnRepository implements AiTurnRepository {
        private final Map<Long, AiTurn> store = new HashMap<>();
        private AiTurn lastUpdated;

        @Override
        public void save(AiTurn aiTurn) {
            store.put(aiTurn.id(), aiTurn);
        }

        @Override
        public int findMaxTurnNoBySessionId(Long sessionId) {
            return store.values().stream()
                    .filter(turn -> turn.sessionId().equals(sessionId))
                    .map(AiTurn::turnNo)
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        @Override
        public void update(AiTurn aiTurn) {
            lastUpdated = aiTurn;
            store.put(aiTurn.id(), aiTurn);
        }
    }

    private static final class InMemoryAiTurnContentRepository implements AiTurnContentRepository {
        private final List<AiTurnContent> savedContents = new ArrayList<>();

        @Override
        public void save(AiTurnContent aiTurnContent) {
            savedContents.add(aiTurnContent);
        }
    }

    private static final class InMemoryAiModelRunRepository implements AiModelRunRepository {
        private final Map<Long, AiModelRun> store = new HashMap<>();
        private AiModelRun lastUpdated;

        @Override
        public void save(AiModelRun aiModelRun) {
            store.put(aiModelRun.id(), aiModelRun);
        }

        @Override
        public void update(AiModelRun aiModelRun) {
            lastUpdated = aiModelRun;
            store.put(aiModelRun.id(), aiModelRun);
        }
    }

    private static final class InMemoryAiGuardrailEventRepository implements AiGuardrailEventRepository {
        private final List<AiGuardrailEvent> savedEvents = new ArrayList<>();

        @Override
        public void save(AiGuardrailEvent aiGuardrailEvent) {
            savedEvents.add(aiGuardrailEvent);
        }
    }
}
