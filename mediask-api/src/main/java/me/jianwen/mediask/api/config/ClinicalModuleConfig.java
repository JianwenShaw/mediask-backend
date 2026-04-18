package me.jianwen.mediask.api.config;

import me.jianwen.mediask.application.clinical.usecase.ListEncountersUseCase;
import me.jianwen.mediask.application.clinical.usecase.GetEncounterDetailUseCase;
import me.jianwen.mediask.domain.clinical.port.EncounterQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClinicalModuleConfig {

    @Bean
    public ListEncountersUseCase listEncountersUseCase(EncounterQueryRepository encounterQueryRepository) {
        return new ListEncountersUseCase(encounterQueryRepository);
    }

    @Bean
    public GetEncounterDetailUseCase getEncounterDetailUseCase(EncounterQueryRepository encounterQueryRepository) {
        return new GetEncounterDetailUseCase(encounterQueryRepository);
    }
}
