package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.query.GetAiSessionDetailQuery;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.AiSessionDetail;
import me.jianwen.mediask.domain.ai.model.AiSessionMessage;
import me.jianwen.mediask.domain.ai.model.AiSessionTurnDetail;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import org.springframework.transaction.annotation.Transactional;

public class GetAiSessionDetailUseCase {

    private final AiSessionQueryRepository aiSessionQueryRepository;
    private final AiContentEncryptorPort aiContentEncryptorPort;

    public GetAiSessionDetailUseCase(
            AiSessionQueryRepository aiSessionQueryRepository, AiContentEncryptorPort aiContentEncryptorPort) {
        this.aiSessionQueryRepository = aiSessionQueryRepository;
        this.aiContentEncryptorPort = aiContentEncryptorPort;
    }

    @Transactional(readOnly = true)
    public AiSessionDetail handle(GetAiSessionDetailQuery query) {
        AiSessionDetail detail = aiSessionQueryRepository
                .findSessionDetailById(query.sessionId())
                .orElseThrow(() -> new BizException(AiErrorCode.AI_SESSION_NOT_FOUND));
        if (!detail.patientId().equals(query.patientUserId())) {
            throw new BizException(AiErrorCode.AI_SESSION_ACCESS_DENIED);
        }
        return new AiSessionDetail(
                detail.sessionId(),
                detail.patientId(),
                detail.departmentId(),
                detail.sceneType(),
                detail.status(),
                detail.chiefComplaintSummary(),
                detail.summary(),
                detail.startedAt(),
                detail.endedAt(),
                detail.turns().stream().map(this::decryptTurn).toList());
    }

    private AiSessionTurnDetail decryptTurn(AiSessionTurnDetail turn) {
        return new AiSessionTurnDetail(
                turn.turnId(),
                turn.turnNo(),
                turn.status(),
                turn.startedAt(),
                turn.completedAt(),
                turn.errorCode(),
                turn.errorMessage(),
                turn.messages().stream().map(this::decryptMessage).toList());
    }

    private AiSessionMessage decryptMessage(AiSessionMessage message) {
        return new AiSessionMessage(
                message.role(), aiContentEncryptorPort.decrypt(message.encryptedContent()), message.createdAt());
    }
}
