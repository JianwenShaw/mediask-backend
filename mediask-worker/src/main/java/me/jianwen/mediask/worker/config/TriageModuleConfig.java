package me.jianwen.mediask.worker.config;

import me.jianwen.mediask.application.triage.usecase.PublishTriageCatalogUseCase;
import me.jianwen.mediask.domain.triage.port.DepartmentCatalogSourcePort;
import me.jianwen.mediask.domain.triage.port.TriageCatalogPublishPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TriageModuleConfig {

    @Bean
    public PublishTriageCatalogUseCase publishTriageCatalogUseCase(
            TriageCatalogPublishPort publishPort, DepartmentCatalogSourcePort sourcePort) {
        return new PublishTriageCatalogUseCase(publishPort, sourcePort);
    }
}
