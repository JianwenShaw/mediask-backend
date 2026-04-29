package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.knowledge.usecase.KnowledgeAdminGatewayUseCase;
import me.jianwen.mediask.domain.ai.port.KnowledgeAdminGatewayPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgeAdminModuleConfig {

    @Bean
    public KnowledgeAdminGatewayUseCase knowledgeAdminGatewayUseCase(KnowledgeAdminGatewayPort gatewayPort) {
        return new KnowledgeAdminGatewayUseCase(gatewayPort);
    }
}
