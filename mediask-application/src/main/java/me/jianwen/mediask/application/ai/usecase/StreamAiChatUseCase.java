package me.jianwen.mediask.application.ai.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.function.Consumer;
import me.jianwen.mediask.application.ai.command.StreamAiChatCommand;
import me.jianwen.mediask.common.exception.BaseException;
import me.jianwen.mediask.common.exception.ErrorCodeType;
import me.jianwen.mediask.common.exception.SysException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatStreamEvent;
import me.jianwen.mediask.domain.ai.model.AiChatTriageResult;
import me.jianwen.mediask.domain.ai.model.AiContentRole;
import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;
import me.jianwen.mediask.domain.ai.model.AiModelRun;
import me.jianwen.mediask.domain.ai.model.AiSession;
import me.jianwen.mediask.domain.ai.model.AiTurn;
import me.jianwen.mediask.domain.ai.model.AiTurnContent;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;

public class StreamAiChatUseCase {

    private final AiChatStreamPort aiChatStreamPort;
    private final AiSessionRepository aiSessionRepository;
    private final AiTurnRepository aiTurnRepository;
    private final AiTurnContentRepository aiTurnContentRepository;
    private final AiModelRunRepository aiModelRunRepository;
    private final AiGuardrailEventRepository aiGuardrailEventRepository;
    private final AiContentEncryptorPort aiContentEncryptorPort;

    public StreamAiChatUseCase(
            AiChatStreamPort aiChatStreamPort,
            AiSessionRepository aiSessionRepository,
            AiTurnRepository aiTurnRepository,
            AiTurnContentRepository aiTurnContentRepository,
            AiModelRunRepository aiModelRunRepository,
            AiGuardrailEventRepository aiGuardrailEventRepository,
            AiContentEncryptorPort aiContentEncryptorPort) {
        this.aiChatStreamPort = aiChatStreamPort;
        this.aiSessionRepository = aiSessionRepository;
        this.aiTurnRepository = aiTurnRepository;
        this.aiTurnContentRepository = aiTurnContentRepository;
        this.aiModelRunRepository = aiModelRunRepository;
        this.aiGuardrailEventRepository = aiGuardrailEventRepository;
        this.aiContentEncryptorPort = aiContentEncryptorPort;
    }

    public void handle(StreamAiChatCommand command, Consumer<AiChatStreamResultEvent> eventConsumer) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");

        AiSession aiSession = loadOrCreateSession(command);
        String inputHash = sha256(command.message());
        AiTurn aiTurn = AiTurn.createProcessing(aiSession.id(), aiTurnRepository.findMaxTurnNoBySessionId(aiSession.id()) + 1, inputHash);
        AiModelRun aiModelRun =
                AiModelRun.createRunning(aiTurn.id(), command.requestId(), hashInvocation(aiSession, aiTurn, command), true);

        aiTurnRepository.save(aiTurn);
        aiModelRunRepository.save(aiModelRun);
        aiTurnContentRepository.save(createContent(aiTurn.id(), AiContentRole.USER, command.message(), inputHash));

        StreamExecution execution = new StreamExecution(aiSession, aiTurn, aiModelRun, inputHash, eventConsumer);
        try {
            aiChatStreamPort.stream(
                    new AiChatInvocation(
                            aiModelRun.id(),
                            aiTurn.id(),
                            aiSession.sessionUuid(),
                            command.message(),
                            aiSession.sceneType(),
                            command.departmentId(),
                            aiSession.summary(),
                            true),
                    execution::handle);
            execution.ensureCompleted();
        } catch (Exception exception) {
            execution.fail(exception);
        }
    }

    private AiSession loadOrCreateSession(StreamAiChatCommand command) {
        if (command.sessionId() == null) {
            AiSession aiSession =
                    AiSession.createActive(command.patientUserId(), command.departmentId(), command.sceneType());
            aiSessionRepository.save(aiSession);
            return aiSession;
        }

        AiSession aiSession = aiSessionRepository
                .findById(command.sessionId())
                .orElseThrow(() -> new me.jianwen.mediask.common.exception.BizException(AiErrorCode.AI_SESSION_NOT_FOUND));
        if (!aiSession.patientId().equals(command.patientUserId())) {
            throw new me.jianwen.mediask.common.exception.BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }
        return aiSession;
    }

    private AiTurnContent createContent(Long turnId, AiContentRole role, String content, String contentHash) {
        return AiTurnContent.create(
                turnId,
                role,
                aiContentEncryptorPort.encrypt(content),
                mask(content),
                contentHash);
    }

    private String mask(String content) {
        String normalized = content == null ? null : content.trim();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String hashInvocation(AiSession aiSession, AiTurn aiTurn, StreamAiChatCommand command) {
        return sha256(
                aiSession.sessionUuid() + "|" + aiTurn.id() + "|" + command.sceneType().name() + "|" + command.message());
    }

    private String hashResponse(AiChatTriageResult triageResult, String answer) {
        return sha256(
                answer
                        + "|"
                        + triageResult.riskLevel().name()
                        + "|"
                        + triageResult.guardrailAction().name()
                        + "|"
                        + triageResult.executionMetadata().providerRunId());
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private final class StreamExecution {

        private final AiSession aiSession;
        private final AiTurn aiTurn;
        private final AiModelRun aiModelRun;
        private final String inputHash;
        private final Consumer<AiChatStreamResultEvent> eventConsumer;
        private final StringBuilder answerBuilder = new StringBuilder();

        private AiChatTriageResult triageResult;
        private boolean terminal;

        private StreamExecution(
                AiSession aiSession,
                AiTurn aiTurn,
                AiModelRun aiModelRun,
                String inputHash,
                Consumer<AiChatStreamResultEvent> eventConsumer) {
            this.aiSession = aiSession;
            this.aiTurn = aiTurn;
            this.aiModelRun = aiModelRun;
            this.inputHash = inputHash;
            this.eventConsumer = eventConsumer;
        }

        private void handle(AiChatStreamEvent event) {
            if (terminal) {
                return;
            }
            switch (event) {
                case AiChatStreamEvent.Message message -> {
                    answerBuilder.append(message.content());
                    eventConsumer.accept(new AiChatStreamResultEvent.Message(message.content()));
                }
                case AiChatStreamEvent.Meta meta -> {
                    if (triageResult != null) {
                        throw new SysException(AiErrorCode.INVALID_RESPONSE, "ai stream meta event duplicated");
                    }
                    triageResult = meta.triageResult();
                    eventConsumer.accept(new AiChatStreamResultEvent.Meta(aiSession.id(), aiTurn.id(), meta.triageResult()));
                }
                case AiChatStreamEvent.End ignored -> succeed();
                case AiChatStreamEvent.Error error -> fail(error.code(), error.message());
            }
        }

        private void ensureCompleted() {
            if (!terminal) {
                fail(new SysException(AiErrorCode.INVALID_RESPONSE, "ai stream ended without terminal event"));
            }
        }

        private void succeed() {
            if (triageResult == null) {
                throw new SysException(AiErrorCode.INVALID_RESPONSE, "ai stream meta event missing");
            }
            String answer = answerBuilder.toString();
            if (answer.isBlank()) {
                throw new SysException(AiErrorCode.INVALID_RESPONSE, "ai stream answer is empty");
            }

            String outputHash = sha256(answer);
            aiTurnContentRepository.save(createContent(aiTurn.id(), AiContentRole.ASSISTANT, answer, outputHash));

            aiSession.updateSummary(triageResult.chiefComplaintSummary(), triageResult.chiefComplaintSummary());
            aiSessionRepository.update(aiSession);

            aiTurn.markCompleted(outputHash);
            aiTurnRepository.update(aiTurn);

            aiModelRun.markSucceeded(triageResult.executionMetadata(), hashResponse(triageResult, answer));
            aiModelRunRepository.update(aiModelRun);

            aiGuardrailEventRepository.save(AiGuardrailEvent.create(
                    aiModelRun.id(),
                    triageResult.riskLevel(),
                    triageResult.guardrailAction(),
                    triageResult.executionMetadata().matchedRuleCodes(),
                    triageResult.chiefComplaintSummary(),
                    triageResult.recommendedDepartments(),
                    triageResult.careAdvice(),
                    inputHash,
                    outputHash));

            terminal = true;
            eventConsumer.accept(new AiChatStreamResultEvent.End());
        }

        private void fail(Exception exception) {
            ErrorCodeType errorCode = resolveErrorCode(exception);
            fail(errorCode.getCode(), exception.getMessage() == null ? errorCode.getMessage() : exception.getMessage());
        }

        private void fail(int code, String message) {
            if (terminal) {
                return;
            }
            aiTurn.markFailed(code, message);
            aiTurnRepository.update(aiTurn);
            aiModelRun.markFailed(code, message);
            aiModelRunRepository.update(aiModelRun);
            terminal = true;
            eventConsumer.accept(new AiChatStreamResultEvent.Error(code, message));
        }

        private ErrorCodeType resolveErrorCode(Exception exception) {
            if (exception instanceof BaseException baseException) {
                return baseException.getErrorCode();
            }
            return AiErrorCode.SERVICE_UNAVAILABLE;
        }
    }
}
