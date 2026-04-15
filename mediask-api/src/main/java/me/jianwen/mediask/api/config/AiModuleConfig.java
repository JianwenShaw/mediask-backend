package me.jianwen.mediask.api.config;

import java.time.Clock;
import me.jianwen.mediask.application.ai.usecase.ChatAiUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionRegistrationHandoffUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatPort;
import me.jianwen.mediask.domain.ai.port.AiContentEncryptorPort;
import me.jianwen.mediask.domain.ai.port.AiGuardrailEventRepository;
import me.jianwen.mediask.domain.ai.port.AiModelRunRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionQueryRepository;
import me.jianwen.mediask.domain.ai.port.AiSessionRepository;
import me.jianwen.mediask.application.ai.usecase.StreamAiChatUseCase;
import me.jianwen.mediask.domain.ai.port.AiChatStreamPort;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import me.jianwen.mediask.infra.observability.MdcTaskDecorator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Bean
    public GetAiSessionDetailUseCase getAiSessionDetailUseCase(
            AiSessionQueryRepository aiSessionQueryRepository, AiContentEncryptorPort aiContentEncryptorPort) {
        return new GetAiSessionDetailUseCase(aiSessionQueryRepository, aiContentEncryptorPort);
    }

    @Bean
    public ListAiSessionsUseCase listAiSessionsUseCase(AiSessionQueryRepository aiSessionQueryRepository) {
        return new ListAiSessionsUseCase(aiSessionQueryRepository);
    }

    @Bean
    public GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase(
            AiSessionQueryRepository aiSessionQueryRepository) {
        return new GetAiSessionTriageResultUseCase(aiSessionQueryRepository);
    }

    @Bean("aiRegistrationHandoffClock")
    public Clock aiRegistrationHandoffClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public GetAiSessionRegistrationHandoffUseCase getAiSessionRegistrationHandoffUseCase(
            AiSessionQueryRepository aiSessionQueryRepository,
            @Qualifier("aiRegistrationHandoffClock") Clock aiRegistrationHandoffClock) {
        return new GetAiSessionRegistrationHandoffUseCase(aiSessionQueryRepository, aiRegistrationHandoffClock);
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
