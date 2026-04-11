package me.jianwen.mediask.application.ai.usecase;

import java.util.Locale;
import me.jianwen.mediask.application.ai.command.CreateKnowledgeBaseCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseOwnerType;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseSummary;
import me.jianwen.mediask.domain.ai.model.KnowledgeBaseVisibility;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.model.KnowledgeBase;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import org.springframework.transaction.annotation.Transactional;

public class CreateKnowledgeBaseUseCase {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public CreateKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional
    public KnowledgeBaseSummary handle(CreateKnowledgeBaseCommand command) {
        validate(command);
        KnowledgeBase knowledgeBase = KnowledgeBase.create(
                command.kbCode(),
                command.name(),
                enumValueOf(command.ownerType(), KnowledgeBaseOwnerType.class, "ownerType"),
                command.ownerDeptId(),
                enumValueOf(command.visibility(), KnowledgeBaseVisibility.class, "visibility"));
        knowledgeBaseRepository.save(knowledgeBase);
        return knowledgeBaseRepository
                .findSummaryById(knowledgeBase.id())
                .orElseThrow(() -> new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    private void validate(CreateKnowledgeBaseCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
    }

    private <T extends Enum<T>> T enumValueOf(String rawValue, Class<T> enumClass, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return Enum.valueOf(enumClass, rawValue.trim().toUpperCase(Locale.ROOT));
    }
}
