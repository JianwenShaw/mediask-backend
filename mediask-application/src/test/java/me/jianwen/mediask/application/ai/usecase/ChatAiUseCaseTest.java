package me.jianwen.mediask.application.ai.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.jianwen.mediask.application.ai.command.ChatAiCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiExecutionMetadata;
import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;
import me.jianwen.mediask.domain.ai.model.AiModelRun;
import me.jianwen.mediask.domain.ai.model.AiSession;
import me.jianwen.mediask.domain.ai.model.AiTurn;
import me.jianwen.mediask.domain.ai.model.AiTurnContent;
import me.jianwen.mediask.domain.ai.model.AiSceneType;
import me.jianwen.mediask.domain.ai.model.AiCitation;
import me.jianwen.mediask.domain.ai.model.GuardrailAction;
import me.jianwen.mediask.domain.ai.model.RecommendedDepartment;
import me.jianwen.mediask.domain.ai.model.RiskLevel;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.junit.jupiter.api.Test;

class ChatAiUseCaseTest {

    @Test
    void handle_WhenNewSession_ShouldPersistConversationAndReturnResult() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiTurnContentRepository turnContentRepository = new InMemoryAiTurnContentRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        InMemoryAiGuardrailEventRepository guardrailEventRepository = new InMemoryAiGuardrailEventRepository();
        ChatAiUseCase useCase = new ChatAiUseCase(
                invocation -> successfulReply(),
                sessionRepository,
                turnRepository,
                turnContentRepository,
                modelRunRepository,
                guardrailEventRepository,
                plainText -> "enc<" + plainText + ">");

        ChatAiResult result = useCase.handle(new ChatAiCommand(
                1001L, null, "头痛三天", 2001L, AiSceneType.PRE_CONSULTATION, "req_chat_001"));

        assertEquals("建议挂神经内科", result.answer());
        assertEquals(1, sessionRepository.store.size());
        assertEquals(1, turnRepository.store.size());
        assertEquals(2, turnContentRepository.savedContents.size());
        assertEquals("enc<头痛三天>", turnContentRepository.savedContents.get(0).encryptedContent());
        assertEquals("enc<建议挂神经内科>", turnContentRepository.savedContents.get(1).encryptedContent());
        assertEquals(1, modelRunRepository.store.size());
        assertEquals(1, guardrailEventRepository.savedEvents.size());
    }

    @Test
    void handle_WhenSessionBelongsToAnotherPatient_ShouldReject() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        AiSession existing = AiSession.createActive(2002L, 3001L, AiSceneType.PRE_CONSULTATION);
        sessionRepository.save(existing);
        ChatAiUseCase useCase = new ChatAiUseCase(
                invocation -> successfulReply(),
                sessionRepository,
                new InMemoryAiTurnRepository(),
                new InMemoryAiTurnContentRepository(),
                new InMemoryAiModelRunRepository(),
                new InMemoryAiGuardrailEventRepository(),
                plainText -> plainText);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ChatAiCommand(
                        1001L, existing.id(), "头痛三天", 2001L, AiSceneType.PRE_CONSULTATION, "req_chat_002")));

        assertEquals(AiErrorCode.AI_SESSION_ACCESS_DENIED.getCode(), exception.getCode());
    }

    @Test
    void handle_WhenAiCallFails_ShouldMarkTurnAndRunFailed() {
        InMemoryAiSessionRepository sessionRepository = new InMemoryAiSessionRepository();
        InMemoryAiTurnRepository turnRepository = new InMemoryAiTurnRepository();
        InMemoryAiModelRunRepository modelRunRepository = new InMemoryAiModelRunRepository();
        ChatAiUseCase useCase = new ChatAiUseCase(
                invocation -> {
                    throw new BizException(AiErrorCode.SERVICE_UNAVAILABLE);
                },
                sessionRepository,
                turnRepository,
                new InMemoryAiTurnContentRepository(),
                modelRunRepository,
                new InMemoryAiGuardrailEventRepository(),
                plainText -> plainText);

        BizException exception = assertThrows(
                BizException.class,
                () -> useCase.handle(new ChatAiCommand(
                        1001L, null, "头痛三天", 2001L, AiSceneType.PRE_CONSULTATION, "req_chat_003")));

        assertEquals(AiErrorCode.SERVICE_UNAVAILABLE.getCode(), exception.getCode());
        assertTrue(turnRepository.lastUpdated.errorCode() != null);
        assertTrue(modelRunRepository.lastUpdated.errorCode() != null);
    }

    private AiChatReply successfulReply() {
        return new AiChatReply(
                "建议挂神经内科",
                "头痛三天",
                RiskLevel.MEDIUM,
                GuardrailAction.CAUTION,
                java.util.List.of(new RecommendedDepartment(101L, "神经内科", 1, "头痛持续")),
                "建议线下就诊",
                java.util.List.of(new AiCitation(7001L, 1, 0.82D, "持续头痛建议线下评估")),
                new AiExecutionMetadata("provider-run-1", java.util.List.of("RISK_HEADACHE"), 100, 200, 1234, false));
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
        private final java.util.List<AiTurnContent> savedContents = new java.util.ArrayList<>();

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
        private final java.util.List<AiGuardrailEvent> savedEvents = new java.util.ArrayList<>();

        @Override
        public void save(AiGuardrailEvent aiGuardrailEvent) {
            savedEvents.add(aiGuardrailEvent);
        }
    }
}
