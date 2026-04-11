package me.jianwen.mediask.application.ai.usecase;

import me.jianwen.mediask.application.ai.command.DeleteKnowledgeDocumentCommand;
import me.jianwen.mediask.common.exception.BizException;
import me.jianwen.mediask.domain.ai.exception.AiErrorCode;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import org.springframework.transaction.annotation.Transactional;

public class DeleteKnowledgeDocumentUseCase {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public DeleteKnowledgeDocumentUseCase(KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    @Transactional
    public void handle(DeleteKnowledgeDocumentCommand command) {
        if (command == null || command.documentId() == null) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (knowledgeDocumentRepository.findById(command.documentId()).isEmpty()) {
            throw new BizException(AiErrorCode.KNOWLEDGE_DOCUMENT_NOT_FOUND);
        }
        knowledgeDocumentRepository.deleteById(command.documentId());
    }
}
