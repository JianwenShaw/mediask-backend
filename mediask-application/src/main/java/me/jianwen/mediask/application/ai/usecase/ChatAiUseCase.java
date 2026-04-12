package me.jianwen.mediask.application.ai.usecase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import me.jianwen.mediask.application.ai.command.ChatAiCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiChatInvocation;
import me.jianwen.mediask.domain.ai.model.AiChatReply;
import me.jianwen.mediask.domain.ai.model.AiContentRole;
import me.jianwen.mediask.domain.ai.model.AiGuardrailEvent;
import me.jianwen.mediask.domain.ai.model.AiModelRun;
import me.jianwen.mediask.domain.ai.model.AiSession;
import me.jianwen.mediask.domain.ai.model.AiTurn;
import me.jianwen.mediask.domain.ai.model.AiTurnContent;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.springframework.transaction.annotation.Transactional;

public class ChatAiUseCase {

    private final AiChatPort aiChatPort;
    private final AiSessionRepository aiSessionRepository;
    private final AiTurnRepository aiTurnRepository;
    private final AiTurnContentRepository aiTurnContentRepository;
    private final AiModelRunRepository aiModelRunRepository;
    private final AiGuardrailEventRepository aiGuardrailEventRepository;
    private final AiContentEncryptorPort aiContentEncryptorPort;

    public ChatAiUseCase(
            AiChatPort aiChatPort,
            AiSessionRepository aiSessionRepository,
            AiTurnRepository aiTurnRepository,
            AiTurnContentRepository aiTurnContentRepository,
            AiModelRunRepository aiModelRunRepository,
            AiGuardrailEventRepository aiGuardrailEventRepository,
            AiContentEncryptorPort aiContentEncryptorPort) {
        this.aiChatPort = aiChatPort;
        this.aiSessionRepository = aiSessionRepository;
        this.aiTurnRepository = aiTurnRepository;
        this.aiTurnContentRepository = aiTurnContentRepository;
        this.aiModelRunRepository = aiModelRunRepository;
        this.aiGuardrailEventRepository = aiGuardrailEventRepository;
        this.aiContentEncryptorPort = aiContentEncryptorPort;
    }

    @Transactional
    public ChatAiResult handle(ChatAiCommand command) {
        AiSession aiSession = loadOrCreateSession(command);
        String inputHash = sha256(command.message());
        AiTurn aiTurn = AiTurn.createProcessing(aiSession.id(), aiTurnRepository.findMaxTurnNoBySessionId(aiSession.id()) + 1, inputHash);
        AiModelRun aiModelRun = AiModelRun.createRunning(aiTurn.id(), command.requestId(), hashInvocation(aiSession, aiTurn, command), true);

        aiTurnRepository.save(aiTurn);
        aiModelRunRepository.save(aiModelRun);
        aiTurnContentRepository.save(createContent(aiTurn.id(), AiContentRole.USER, command.message(), inputHash));

        try {
            AiChatReply reply = aiChatPort.chat(new AiChatInvocation(
                    aiModelRun.id(),
                    aiTurn.id(),
                    aiSession.sessionUuid(),
                    command.message(),
                    aiSession.sceneType(),
                    command.departmentId(),
                    aiSession.summary(),
                    true));

            String outputHash = sha256(reply.answer());
            aiTurnContentRepository.save(createContent(aiTurn.id(), AiContentRole.ASSISTANT, reply.answer(), outputHash));

            aiSession.updateSummary(reply.chiefComplaintSummary(), reply.chiefComplaintSummary());
            aiSessionRepository.update(aiSession);

            aiTurn.markCompleted(outputHash);
            aiTurnRepository.update(aiTurn);

            aiModelRun.markSucceeded(reply.executionMetadata(), hashReply(reply));
            aiModelRunRepository.update(aiModelRun);

            aiGuardrailEventRepository.save(AiGuardrailEvent.create(
                    aiModelRun.id(),
                    reply.riskLevel(),
                    reply.guardrailAction(),
                    reply.executionMetadata().matchedRuleCodes(),
                    inputHash,
                    outputHash));

            return new ChatAiResult(aiSession.id(), aiTurn.id(), reply.answer(), reply);
        } catch (BizException exception) {
            markFailed(aiTurn, aiModelRun, exception.getCode(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            markFailed(aiTurn, aiModelRun, AiErrorCode.SERVICE_UNAVAILABLE.getCode(), exception.getMessage());
            throw exception;
        }
    }

    private AiSession loadOrCreateSession(ChatAiCommand command) {
        if (command.sessionId() == null) {
            AiSession aiSession = AiSession.createActive(command.patientUserId(), command.departmentId(), command.sceneType());
            aiSessionRepository.save(aiSession);
            return aiSession;
        }

        AiSession aiSession = aiSessionRepository
                .findById(command.sessionId())
                .orElseThrow(() -> new BizException(AiErrorCode.AI_SESSION_NOT_FOUND));
        if (!aiSession.patientId().equals(command.patientUserId())) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
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

    private void markFailed(AiTurn aiTurn, AiModelRun aiModelRun, int errorCode, String errorMessage) {
        aiTurn.markFailed(errorCode, errorMessage);
        aiTurnRepository.update(aiTurn);
        aiModelRun.markFailed(errorCode, errorMessage);
        aiModelRunRepository.update(aiModelRun);
    }

    private String mask(String content) {
        String normalized = content == null ? null : content.trim();
        if (normalized == null || normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private String hashInvocation(AiSession aiSession, AiTurn aiTurn, ChatAiCommand command) {
        return sha256(
                aiSession.sessionUuid() + "|" + aiTurn.id() + "|" + command.sceneType().name() + "|" + command.message());
    }

    private String hashReply(AiChatReply reply) {
        return sha256(
                reply.answer()
                        + "|"
                        + reply.riskLevel().name()
                        + "|"
                        + reply.guardrailAction().name()
                        + "|"
                        + reply.executionMetadata().providerRunId());
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
