package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.ImportKnowledgeDocumentUseCase;
import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.KnowledgeBaseRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeChunkRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentRepository;
import me.jianwen.mediask.domain.ai.port.KnowledgeDocumentStoragePort;
import me.jianwen.mediask.domain.ai.port.KnowledgeIndexPort;
import me.jianwen.mediask.domain.ai.port.KnowledgePreparePort;
import me.jianwen.mediask.infra.observability.MdcTaskDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiModuleConfig {

    @Bean
    public StreamAiChatUseCase streamAiChatUseCase(AiChatStreamPort aiChatStreamPort) {
        return new StreamAiChatUseCase(aiChatStreamPort);
    }

    @Bean
    @ConditionalOnBean({
        KnowledgeBaseRepository.class,
        KnowledgeDocumentRepository.class,
        KnowledgeChunkRepository.class,
        KnowledgeDocumentStoragePort.class,
        KnowledgePreparePort.class,
        KnowledgeIndexPort.class,
        PlatformTransactionManager.class
    })
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

    @Bean(name = "aiSseTaskExecutor")
    public TaskExecutor aiSseTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("ai-sse-");
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setQueueCapacity(32);
        taskExecutor.setTaskDecorator(new MdcTaskDecorator());
        taskExecutor.initialize();
        return taskExecutor;
    }
}
