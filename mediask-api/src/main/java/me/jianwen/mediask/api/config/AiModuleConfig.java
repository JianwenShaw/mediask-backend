package me.jianwen.mediask.api.config;

import java.time.Clock;
import me.jianwen.mediask.application.ai.usecase.AiRegistrationHandoffSupport;
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
import me.jianwen.mediask.domain.ai.port.TriageDepartmentCatalogPort;
import me.jianwen.mediask.domain.ai.port.AiTurnContentRepository;
import me.jianwen.mediask.domain.ai.port.AiTurnRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            AiContentEncryptorPort aiContentEncryptorPort,
            TriageDepartmentCatalogPort triageDepartmentCatalogPort) {
        return new ChatAiUseCase(
                aiChatPort,
                aiSessionRepository,
                aiTurnRepository,
                aiTurnContentRepository,
                aiModelRunRepository,
                aiGuardrailEventRepository,
                aiContentEncryptorPort,
                triageDepartmentCatalogPort);
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
            AiRegistrationHandoffSupport aiRegistrationHandoffSupport,
            @Qualifier("aiRegistrationHandoffClock") Clock aiRegistrationHandoffClock) {
        return new GetAiSessionRegistrationHandoffUseCase(aiRegistrationHandoffSupport, aiRegistrationHandoffClock);
    }
}
