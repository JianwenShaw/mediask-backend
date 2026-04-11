package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.CreateKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeBaseUseCase;
import me.jianwen.mediask.application.ai.usecase.DeleteKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeBasesUseCase;
import me.jianwen.mediask.application.ai.usecase.ListKnowledgeDocumentsUseCase;
import me.jianwen.mediask.application.ai.usecase.UpdateKnowledgeBaseUseCase;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiAdminModuleConfig {

    @Bean
    public ListKnowledgeBasesUseCase listKnowledgeBasesUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        return new ListKnowledgeBasesUseCase(knowledgeBaseRepository);
    }

    @Bean
    public CreateKnowledgeBaseUseCase createKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        return new CreateKnowledgeBaseUseCase(knowledgeBaseRepository);
    }

    @Bean
    public UpdateKnowledgeBaseUseCase updateKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        return new UpdateKnowledgeBaseUseCase(knowledgeBaseRepository);
    }

    @Bean
    public DeleteKnowledgeBaseUseCase deleteKnowledgeBaseUseCase(KnowledgeBaseRepository knowledgeBaseRepository) {
        return new DeleteKnowledgeBaseUseCase(knowledgeBaseRepository);
    }

    @Bean
    public ListKnowledgeDocumentsUseCase listKnowledgeDocumentsUseCase(
            KnowledgeDocumentRepository knowledgeDocumentRepository) {
        return new ListKnowledgeDocumentsUseCase(knowledgeDocumentRepository);
    }

    @Bean
    public DeleteKnowledgeDocumentUseCase deleteKnowledgeDocumentUseCase(
            KnowledgeDocumentRepository knowledgeDocumentRepository) {
        return new DeleteKnowledgeDocumentUseCase(knowledgeDocumentRepository);
    }
}
