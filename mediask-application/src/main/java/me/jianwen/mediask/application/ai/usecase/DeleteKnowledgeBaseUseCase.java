package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.command.DeleteKnowledgeBaseCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteKnowledgeBaseUseCase {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public DeleteKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Transactional
    public void handle(DeleteKnowledgeBaseCommand command) {
        if (command == null || command.knowledgeBaseId() == null) {
            throw new IllegalArgumentException("knowledgeBaseId is required");
        }
        if (knowledgeBaseRepository.findById(command.knowledgeBaseId()).isEmpty()) {
            throw new BizException(AiErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }
        knowledgeBaseRepository.deleteById(command.knowledgeBaseId());
    }
}
