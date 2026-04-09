package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
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
    public StreamAiChatUseCase streamAiChatUseCase(AiChatStreamPort aiChatStreamPort) {
        return new StreamAiChatUseCase(aiChatStreamPort);
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
