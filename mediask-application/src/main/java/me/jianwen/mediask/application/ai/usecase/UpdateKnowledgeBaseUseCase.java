package me.jianwen.mediask.application.ai.usecase;

import java.util.Locale;
import me.jianwen.mediask.application.ai.command.UpdateKnowledgeBaseCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseOwnerType;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseStatus;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseVisibility;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import org.springframework.transaction.annotation.Transactional;

public class UpdateKnowledgeBaseUseCase {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public UpdateKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional
    public KnowledgeBaseSummary handle(UpdateKnowledgeBaseCommand command) {
        validate(command);
        KnowledgeBase knowledgeBase = knowledgeBaseRepository
                .findById(command.knowledgeBaseId())
                .orElseThrow(() -> new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
        knowledgeBase.patch(
                command.name(),
                enumValueOf(command.ownerType(), KnowledgeBaseOwnerType.class),
                command.ownerDeptId(),
                enumValueOf(command.visibility(), KnowledgeBaseVisibility.class),
                enumValueOf(command.status(), KnowledgeBaseStatus.class));
        knowledgeBaseRepository.update(knowledgeBase);
        return knowledgeBaseRepository
                .findSummaryById(knowledgeBase.id())
                .orElseThrow(() -> new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    private void validate(UpdateKnowledgeBaseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (command.knowledgeBaseId() == null) {
            throw new IllegalArgumentException("knowledgeBaseId is required");
        }
    }

    private <T extends Enum<T>> T enumValueOf(String rawValue, Class<T> enumClass) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return Enum.valueOf(enumClass, rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
