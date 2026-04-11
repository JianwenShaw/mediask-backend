package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.ImportKnowledgeDocumentUseCase;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiKnowledgeImportConfig {

    @Bean
    public ImportKnowledgeDocumentUseCase importKnowledgeDocumentUseCase(
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository knowledgeDocumentRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            KnowledgeDocumentStoragePort knowledgeDocumentStoragePort,
            KnowledgePreparePort knowledgePreparePort,
            KnowledgeIndexPort knowledgeIndexPort,
            PlatformTransactionManager transactionManager) {
        return new ImportKnowledgeDocumentUseCase(
                knowledgeBaseRepository,
                knowledgeDocumentRepository,
                knowledgeChunkRepository,
                knowledgeDocumentStoragePort,
                knowledgePreparePort,
                knowledgeIndexPort,
                new TransactionTemplate(transactionManager));
    }
}
