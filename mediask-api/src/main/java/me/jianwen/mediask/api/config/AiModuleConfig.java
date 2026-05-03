package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.audit.usecase.AuditTrailService;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionDetailUseCase;
import me.jianwen.mediask.application.ai.usecase.GetAiSessionTriageResultUseCase;
import me.jianwen.mediask.application.ai.usecase.ListAiSessionsUseCase;
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

    @Bean
    public ListAiSessionsUseCase listAiSessionsUseCase(AiTriageGatewayPort aiTriageGatewayPort) {
        return new ListAiSessionsUseCase(aiTriageGatewayPort);
    }

    @Bean
    public GetAiSessionDetailUseCase getAiSessionDetailUseCase(
            AiTriageGatewayPort aiTriageGatewayPort, AuditTrailService auditTrailService) {
        return new GetAiSessionDetailUseCase(aiTriageGatewayPort, auditTrailService);
    }

    @Bean
    public GetAiSessionTriageResultUseCase getAiSessionTriageResultUseCase(
            AiTriageGatewayPort aiTriageGatewayPort, AuditTrailService auditTrailService) {
        return new GetAiSessionTriageResultUseCase(aiTriageGatewayPort, auditTrailService);
    }
}
