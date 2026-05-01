package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.ai.usecase.StreamAiTriageQueryUseCase;
import me.jianwen.mediask.application.ai.usecase.SubmitAiTriageQueryUseCase;
import me.jianwen.mediask.domain.ai.port.AiTriageGatewayPort;
import me.jianwen.mediask.domain.ai.port.AiTriageResultSnapshotRepository;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModuleConfig {

    @Bean
    public SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            AiTriageResultSnapshotRepository aiTriageResultSnapshotRepository,
            TriageCatalogPublishPort triageCatalogPublishPort) {
        return new SubmitAiTriageQueryUseCase(
                aiTriageGatewayPort, aiTriageResultSnapshotRepository, triageCatalogPublishPort);
    }

    @Bean
    public StreamAiTriageQueryUseCase streamAiTriageQueryUseCase(
            AiTriageGatewayPort aiTriageGatewayPort,
            SubmitAiTriageQueryUseCase submitAiTriageQueryUseCase) {
        return new StreamAiTriageQueryUseCase(aiTriageGatewayPort, submitAiTriageQueryUseCase);
    }
}
