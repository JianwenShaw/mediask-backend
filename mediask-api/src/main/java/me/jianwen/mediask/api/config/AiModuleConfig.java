package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import me.jianwen.mediask.infra.observability.MdcTaskDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnProperty(prefix = "mediask.ai.service", name = {"base-url", "api-key"})
public class AiModuleConfig {

    @Bean
    public ChatAiUseCase chatAiUseCase(
            AiChatPort aiChatPort,
            AiSessionRepository aiSessionRepository,
            AiTurnRepository aiTurnRepository,
            AiTurnContentRepository aiTurnContentRepository,
            AiModelRunRepository aiModelRunRepository,
            AiGuardrailEventRepository aiGuardrailEventRepository,
            AiContentEncryptorPort aiContentEncryptorPort) {
        return new ChatAiUseCase(
                aiChatPort,
                aiSessionRepository,
                aiTurnRepository,
                aiTurnContentRepository,
                aiModelRunRepository,
                aiGuardrailEventRepository,
                aiContentEncryptorPort);
    }

    @Bean
    public StreamAiChatUseCase streamAiChatUseCase(
            AiChatStreamPort aiChatStreamPort,
            AiSessionRepository aiSessionRepository,
            AiTurnRepository aiTurnRepository,
            AiTurnContentRepository aiTurnContentRepository,
            AiModelRunRepository aiModelRunRepository,
            AiGuardrailEventRepository aiGuardrailEventRepository,
            AiContentEncryptorPort aiContentEncryptorPort) {
        return new StreamAiChatUseCase(
                aiChatStreamPort,
                aiSessionRepository,
                aiTurnRepository,
                aiTurnContentRepository,
                aiModelRunRepository,
                aiGuardrailEventRepository,
                aiContentEncryptorPort);
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
